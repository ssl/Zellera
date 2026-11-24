package zel.game.moderation

import zel.core.Configuration
import zel.game.players.Player
import zel.game.players.Players
import zel.protocol.generated.ModeratorCautionOut
import zel.protocol.generated.ModeratorCautionOutCodec
import zel.util.SECONDS_PER_HOUR
import javax.sql.DataSource

/**
 * The sanction ladder: each new sanction during a probation is the next step up, the top step repeats. The default
 * sanction for a report is one decision, shown before it is given and given as it was shown.
 */
class Sanctions internal constructor(
    private val table: SanctionTable,
    private val players: Players,
    private val moderation: ModerationService,
    private val config: Configuration,
    private val clock: () -> Int,
) {
    constructor(
        dataSource: DataSource,
        players: Players,
        moderation: ModerationService,
        config: Configuration,
        clock: () -> Int,
    ) : this(SanctionTable(dataSource), players, moderation, config, clock)

    private val ladder: Map<Int, LadderStep> = table.ladder()

    fun step(number: Int): LadderStep? = ladder[number]

    fun latestOf(playerId: Int): SanctionRecord? = table.latestOf(playerId)

    fun endProbation(recordId: Int) = table.endProbation(recordId)

    /** The step the player stands on: their last one while its probation runs, else 0. */
    fun standingOf(playerId: Int): Int = latestOf(playerId)?.takeIf { it.onProbation(clock()) }?.step ?: 0

    /** What a new sanction would be; null only when the ladder is empty. */
    fun nextFor(playerId: Int): LadderStep? = after(standingOf(playerId))

    private fun after(standing: Int): LadderStep? = ladder[standing + 1] ?: ladder.values.lastOrNull()

    /**
     * The default sanction for a report on [topicId] against [playerId]: with the ladder on the player's next step,
     * with it off the topic's preset; an alert when there is neither.
     */
    fun defaultFor(
        topicId: Int,
        playerId: Int,
    ): DefaultMeasure {
        val step = if (config.getBoolean(LADDER_KEY, true)) nextFor(playerId) else null
        if (step != null) return DefaultMeasure(step.kind, hoursOf(step), step)
        return moderation.catalog
            .topic(topicId)
            ?.defaultSanction
            ?.let(::measureOf) ?: ALERT
    }

    /** A preset that names both lengths is a ban. */
    private fun measureOf(preset: SanctionPreset): DefaultMeasure? =
        when {
            preset.banHours > 0 -> DefaultMeasure(SanctionKind.BAN, preset.banHours)
            preset.muteHours > 0 -> DefaultMeasure(SanctionKind.MUTE, muteHours(preset.muteHours))
            else -> null
        }

    private fun hoursOf(step: LadderStep): Int =
        if (step.kind == SanctionKind.MUTE) muteHours(step.hours) else step.hours

    /** A mute is at least an hour and never longer than the cap on staff mutes. */
    private fun muteHours(asked: Int): Int = asked.coerceIn(1, maxOf(1, moderation.mutes.capHours))

    /**
     * Gives [playerId] the default sanction of [topicId]; false when [by] does not reach them or it did not take.
     * With the ladder on it is recorded as the player's next step.
     */
    fun giveDefault(
        playerId: Int,
        by: Player,
        topicId: Int,
        reason: String?,
    ): Boolean {
        if (!moderation.reach.canModerate(by, playerId)) return false
        val measure = defaultFor(topicId, playerId)
        val now = clock()
        val shown = moderation.notice.reason(reason, topicId)
        val endsAt = apply(measure, playerId, by, shown, topicId, now) ?: return false
        measure.step?.let { step ->
            val record =
                SanctionRecord(
                    id = 0,
                    playerId = playerId,
                    step = step.step,
                    kind = measure.kind,
                    reason = shown,
                    issuedAt = now,
                    endsAt = endsAt,
                    probationEndsAt = SanctionLadderMath.probationEnd(now, step.probationDays),
                )
            table.insert(record, by.profile.id, topicId)
        }
        if (measure.kind != SanctionKind.BAN) moderation.standing.count(playerId, StandingCounter.WARNINGS_RECEIVED)
        return true
    }

    /** When the measure ends (0 for an alert), null when it did not take. */
    private fun apply(
        measure: DefaultMeasure,
        playerId: Int,
        by: Player,
        shown: String,
        topicId: Int,
        now: Int,
    ): Int? =
        when (measure.kind) {
            SanctionKind.ALERT -> {
                // an offline player finds the alert in their sanction status
                tell(playerId, shown)
                0
            }

            SanctionKind.MUTE -> {
                val seconds = moderation.mutes.give(playerId, measure.hours * SECONDS_PER_HOUR)
                if (seconds > 0) tell(playerId, moderation.notice.mute(shown, seconds))
                (now + seconds).takeIf { seconds > 0 }
            }

            SanctionKind.BAN -> {
                val seconds = SanctionLadderMath.banSeconds(measure.hours, now)
                (now + seconds).takeIf { moderation.banning.ban(playerId, by, shown, seconds, topicId).isNotEmpty() }
            }
        }

    private fun tell(
        playerId: Int,
        message: String,
    ) {
        players.find(playerId)?.client?.send(ModeratorCautionOutCodec, ModeratorCautionOut(message, ""))
    }

    private companion object {
        val ALERT = DefaultMeasure(SanctionKind.ALERT, 0)
        const val LADDER_KEY = "moderation.sanction_ladder.is_on"
    }
}
