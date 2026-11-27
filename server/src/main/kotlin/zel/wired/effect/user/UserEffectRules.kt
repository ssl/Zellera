package zel.wired.effect.user

import zel.game.players.inventory.KnownBadges
import zel.room.Room
import zel.room.furni.place.Interactions
import zel.room.games.TeamColor
import zel.wired.box.BoxSettings
import zel.wired.firing.FurniSource
import zel.wired.form.BoxValidation
import zel.wired.form.SaveOutcome
import zel.wired.form.SaveRequest
import zel.wired.link.FrameSaves
import zel.wired.signal.FrameText
import kotlin.math.abs
import kotlin.math.absoluteValue

class UserEffectRules(
    private val superWired: (Int) -> Boolean = { false },
    private val filterText: (Int, String) -> String = { _, text -> text },
    private val maxMessage: () -> Int = { MAX_MESSAGE },
    private val maxLines: () -> Int = { MAX_LINES },
    /** Whether the game has that badge code ([zel.game.players.inventory.KnownBadges]). */
    private val badgeExists: (String) -> Boolean = { false },
) {
    val validations: Map<String, BoxValidation> =
        mapOf(
            "wf_act_show_message" to BoxValidation.fromRequest { s, r -> showMessage(s, r.saverId) },
            "wf_act_send_message" to BoxValidation.fromRequest { s, r -> sentMessage(s, r.saverId) },
            "wf_act_send_dataframe" to BoxValidation.fromRequest { s, r -> sentFrame(s, r.saverId) },
            "wf_act_click_conf" to BoxValidation { _, s -> SaveOutcome.Saved(clickConf(s)) },
            "wf_act_join_team" to BoxValidation.fromRequest(::joinTeam),
            "wf_act_freeze" to BoxValidation { _, s -> freeze(s) },
            "wf_act_give_score" to BoxValidation.fromRequest { s, r -> score(s, r, INVALID_SCORE) },
            "wf_act_give_score_tm" to BoxValidation.fromRequest(::teamScore),
            "wf_act_mute_triggerer" to BoxValidation.fromRequest { s, r -> SaveOutcome.Saved(mute(s, r.saverId)) },
            "wf_act_kick_user" to
                BoxValidation.fromRequest { s, r -> SaveOutcome.Saved(s.copy(text = kickText(s, r.saverId))) },
            "wf_act_control_clock" to BoxValidation(::clockPicks),
            "wf_act_adjust_clock" to BoxValidation(::clockPicks),
            "wf_act_teleport_to_room" to BoxValidation(::linkerPicks),
            "wf_act_place_furni" to BoxValidation(::cloneSources),
            "wf_act_set_visibility" to BoxValidation { _, s -> visibility(s) },
            "wf_act_set_highlight" to BoxValidation { _, s -> highlight(s) },
            GiveReward.CODE to BoxValidation.fromRequest(::giveReward),
            "wf_act_log" to BoxValidation { _, s -> SaveOutcome.Saved(logLine(s)) },
            "wf_act_neg_log" to BoxValidation { _, s -> SaveOutcome.Saved(logLine(s)) },
        )

    /** The text is cut at [WriteLog.MAX_TEXT]; a type outside 0 to [WriteLog.MAX_LEVEL] saves as 0. */
    private fun logLine(settings: BoxSettings): BoxSettings =
        settings.copy(
            text = settings.text.take(WriteLog.MAX_TEXT),
            ints = listOf(settings.ints.getOrElse(0) { 0 }.takeIf { it in 0..WriteLog.MAX_LEVEL } ?: 0),
        )

    /**
     * Staff only; the box gives badges only, so a row with any other reward is refused, and so is a badge the game
     * does not have.
     */
    private fun giveReward(
        settings: BoxSettings,
        request: SaveRequest,
    ): SaveOutcome {
        val rewards = StaffReward.parse(settings.text)
        val missing by lazy { rewards.firstOrNull { !badgeExists(it.code) } }
        return when {
            !superWired(request.saverId) -> SaveOutcome.Invalid(SAVE_FAILED, whisper = NOT_ALLOWED)
            rewards.any { !it.isBadge } -> SaveOutcome.Invalid(SAVE_FAILED)
            missing != null -> SaveOutcome.Invalid(SAVE_FAILED, whisper = missing?.let { KnownBadges.missing(it.code) })
            else -> SaveOutcome.Saved(settings)
        }
    }

    /** A text longer than [MAX_SENT_MESSAGE] is refused and the box keeps its old one; a shorter one is cut. */
    private fun showMessage(
        settings: BoxSettings,
        saverId: Int,
    ): SaveOutcome {
        if (settings.text.length > MAX_SENT_MESSAGE) {
            return SaveOutcome.Invalid(TOO_MUCH_TEXT, listOf(LIMIT to MAX_SENT_MESSAGE.toString()))
        }
        // A carriage return is a space; more than the line limit is refused, not cut.
        val typed = settings.text.replace('\r', ' ')
        if (typed.count { it == '\n' } >= maxLines()) return SaveOutcome.Invalid(TOO_MANY_NEWLINES)
        val visibility = (settings.ints[ShowMessage.VISIBILITY]).coerceIn(0, 1)
        val style = settings.ints[ShowMessage.STYLE].takeIf { it in STYLES } ?: FALLBACK_STYLE
        val width = settings.ints[ShowMessage.WIDTH].coerceIn(-1, MAX_WIDTH)
        val text = messageText(filterText(saverId, typed))
        return SaveOutcome.Saved(settings.copy(ints = listOf(visibility, style, width), text = text))
    }

    /** What leaves the room through a Link is other rooms' chat: held to show message's text rules. */
    private fun sentMessage(
        settings: BoxSettings,
        saverId: Int,
    ): SaveOutcome =
        if (settings.text.length > MAX_SENT_MESSAGE) {
            SaveOutcome.Invalid(TOO_MUCH_TEXT, listOf(LIMIT to MAX_SENT_MESSAGE.toString()))
        } else {
            SaveOutcome.Saved(settings.copy(text = filterText(saverId, settings.text)))
        }

    private fun sentFrame(
        settings: BoxSettings,
        saverId: Int,
    ): SaveOutcome =
        when (val checked = FrameSaves.sent(settings)) {
            is SaveOutcome.Saved -> {
                val text = FrameText.withMessages(checked.settings.text) { messageText(filterText(saverId, it)) }
                SaveOutcome.Saved(checked.settings.copy(text = text))
            }

            else -> {
                checked
            }
        }

    private fun clickConf(settings: BoxSettings): BoxSettings {
        val avatars = settings.ints[0].takeIf { it in 0..MAX_AVATAR_CLICKS } ?: 0
        val furni = settings.ints[1].takeIf { it in 0..1 } ?: 0
        return settings.copy(ints = listOf(avatars, furni))
    }

    private fun messageText(text: String): String =
        text
            .lineSequence()
            .take(maxLines())
            .joinToString("\n")
            .take(maxMessage())

    private fun joinTeam(
        settings: BoxSettings,
        request: SaveRequest,
    ): SaveOutcome =
        if (request.ints.isEmpty() || TeamColor.fromCode(settings.ints[JoinTeam.TEAM]) == null) {
            SaveOutcome.Invalid(INVALID_TEAM)
        } else {
            SaveOutcome.Saved(settings)
        }

    /** Only the effects the dialog offers save; any other index is refused and the box keeps its old settings. */
    private fun freeze(settings: BoxSettings): SaveOutcome =
        if (settings.ints.getOrElse(FreezeUser.EFFECT_INDEX) { 0 } in FreezeUser.EFFECTS.indices) {
            SaveOutcome.Saved(settings)
        } else {
            SaveOutcome.Invalid(INVALID_FREEZE_EFFECT)
        }

    private fun score(
        settings: BoxSettings,
        request: SaveRequest,
        error: String,
    ): SaveOutcome {
        val score = settings.ints[GiveScore.SCORE]
        return if (request.ints.isEmpty() || score == 0 || abs(score.toLong()) > MAX_SCORE) {
            SaveOutcome.Invalid(error)
        } else {
            SaveOutcome.Saved(settings)
        }
    }

    private fun teamScore(
        settings: BoxSettings,
        request: SaveRequest,
    ): SaveOutcome =
        when (val scored = score(settings, request, INVALID_POINTS)) {
            is SaveOutcome.Saved -> {
                if (TeamColor.fromCode(settings.ints[GiveTeamScore.TEAM]) ==
                    null
                ) {
                    SaveOutcome.Invalid(INVALID_TEAM)
                } else {
                    scored
                }
            }

            else -> {
                scored
            }
        }

    private fun mute(
        settings: BoxSettings,
        saverId: Int,
    ): BoxSettings =
        settings.copy(
            ints =
                settings.ints.mapIndexed { i, v ->
                    if (i == 0) v.coerceIn(MuteTriggerer.MIN_MINUTES, MuteTriggerer.MAX_MINUTES) else v
                },
            text = kickText(settings, saverId),
        )

    private fun kickText(
        settings: BoxSettings,
        saverId: Int,
    ): String = filterText(saverId, settings.text).replace(LINE_BREAK, " ").take(MAX_KICK_TEXT)

    private fun clockPicks(
        room: Room,
        settings: BoxSettings,
    ): SaveOutcome =
        if (picksOf(room, settings).all { it in CLOCKS }) {
            SaveOutcome.Saved(settings)
        } else {
            SaveOutcome.Invalid(REQUIRE_CLOCK)
        }

    private fun linkerPicks(
        room: Room,
        settings: BoxSettings,
    ): SaveOutcome {
        val picked = settings.furniSources.firstOrNull() == FurniSource.PICKED
        return if (!picked || picksOf(room, settings).all { it == TeleportToRoom.LINKER }) {
            SaveOutcome.Saved(settings)
        } else {
            SaveOutcome.Invalid(REQUIRE_LINKER)
        }
    }

    private fun cloneSources(
        room: Room,
        settings: BoxSettings,
    ): SaveOutcome {
        val source = settings.furniSources.firstOrNull() ?: FurniSource.PICKED
        val picked = source == FurniSource.PICKED || source == FurniSource.SNAPSHOT
        val refused = picked && settings.picks.any { pick -> room.furni[pick]?.let(CloneSources::allows) != true }
        return if (refused) SaveOutcome.Invalid(CloneSources.REFUSED) else SaveOutcome.Saved(settings)
    }

    private fun picksOf(
        room: Room,
        settings: BoxSettings,
    ): List<String> =
        settings.picks.mapNotNull { pick -> room.furni[pick.absoluteValue]?.let { Interactions.of(it.definition) } }

    private fun visibility(settings: BoxSettings): SaveOutcome {
        val ints = settings.ints
        val level = ints[SetVisibility.LEVEL]
        if (level != 0 && level != OFF_OR_FULL) return SaveOutcome.Invalid(INVALID_TRANSPARENCY)
        return SaveOutcome.Saved(
            settings.copy(
                ints =
                    listOf(
                        flag(ints[SetVisibility.EVERYONE]),
                        level,
                        flag(ints[SetVisibility.CLICK_THROUGH]),
                        ints[SetVisibility.EASING].coerceIn(0, MAX_EASING),
                        flag(ints[SetVisibility.TARGET_USERS]),
                    ),
            ),
        )
    }

    private fun highlight(settings: BoxSettings): SaveOutcome {
        val ints = settings.ints
        val type = ints[SetHighlight.TYPE]
        val strength = ints[SetHighlight.STRENGTH]
        val color = ints[SetHighlight.COLOR]
        val animation = ints[SetHighlight.ANIMATION].coerceIn(0, 1)
        return when {
            type !in 0..MAX_TYPE -> {
                SaveOutcome.Invalid(HIGHLIGHT + "invalid_type")
            }

            !strengthFits(type, strength) -> {
                SaveOutcome.Invalid(HIGHLIGHT + "invalid_strength")
            }

            type == BRIGHTEN && animation != PULSE -> {
                SaveOutcome.Invalid(HIGHLIGHT + "pulse_only")
            }

            !colorFits(type, color) -> {
                SaveOutcome.Invalid(HIGHLIGHT + "invalid_color")
            }

            else -> {
                SaveOutcome.Saved(
                    settings.copy(
                        ints =
                            listOf(
                                flag(ints[SetHighlight.EVERYONE]),
                                type,
                                strength,
                                color,
                                animation,
                                ints[SetHighlight.EASING].coerceIn(0, MAX_EASING),
                                flag(ints[SetHighlight.TARGET_USERS]),
                            ),
                    ),
                )
            }
        }
    }

    private fun strengthFits(
        type: Int,
        strength: Int,
    ): Boolean =
        when (type) {
            DARKEN -> strength == 0
            TINT -> strength in 0..OFF_OR_FULL && strength % TINT_STEP == 0
            BRIGHTEN -> strength == 0 || strength == BRIGHTEN_LEVEL
            else -> strength == 0 || strength == OFF_OR_FULL
        }

    private fun colorFits(
        type: Int,
        color: Int,
    ): Boolean =
        when (type) {
            DARKEN, BRIGHTEN -> true
            TINT -> color != WHITE && color in COLORS
            else -> color in COLORS
        }

    private fun flag(value: Int): Int = if (value != 0) 1 else 0

    companion object {
        const val SAVE_FAILED = "This effect cannot be saved with these settings."
        const val NOT_ALLOWED = "You don't have the rights to save this reward."
        const val INVALID_TEAM = "Choose one of the teams."
        const val INVALID_SCORE = "That score is out of range."
        const val INVALID_POINTS = "That number of points is out of range."
        const val INVALID_FREEZE_EFFECT = "wiredfurni.error.invalid_freeze_effect"
        const val REQUIRE_CLOCK = "wiredfurni.error.require_counter_furni"
        const val REQUIRE_LINKER = "wiredfurni.error.require_room_linker"
        const val INVALID_TRANSPARENCY = "wiredfurni.error.set_furni_transparency.invalid_transparency"
        const val HIGHLIGHT = "wiredfurni.error.set_furni_highlight."
        const val TOO_MUCH_TEXT = "wiredfurni.error.too_much_text"
        const val TOO_MANY_NEWLINES = "wiredfurni.error.too_many_newlines"
        const val LIMIT = "limit"

        const val MAX_SCORE = 100L
        const val MAX_MESSAGE = 400
        const val MAX_SENT_MESSAGE = 1000
        const val MAX_KICK_TEXT = 200
        const val MAX_LINES = 8
        const val MAX_WIDTH = 2
        const val MAX_AVATAR_CLICKS = 2
        const val FALLBACK_STYLE = 34
        const val OFF_OR_FULL = 100
        const val MAX_EASING = 4
        const val MAX_TYPE = 5
        const val DARKEN = 2
        const val TINT = 3
        const val BRIGHTEN = 4
        const val TINT_STEP = 25
        const val BRIGHTEN_LEVEL = 38
        const val PULSE = 1
        const val WHITE = 0xFFFFFF

        val LINE_BREAK = Regex("[\r\n]")
        val CLOCKS = setOf("clock.timer", "clock.counter.game", "clock.counter.plain", "clock.hourglass")
        val STYLES = setOf(34, 200, 201, 202, 210, 211, 212) + (220..229) + setOf(250, 251, 252)
        val COLORS =
            setOf(
                WHITE,
                16771584,
                16758528,
                16747008,
                16734774,
                16724016,
                16724855,
                16733627,
                15741144,
                12076287,
                7883007,
                4611327,
                2395391,
                2146303,
                2156016,
                1628592,
                2149480,
                5300272,
                10546464,
                16557530,
                12291417,
            )
    }
}
