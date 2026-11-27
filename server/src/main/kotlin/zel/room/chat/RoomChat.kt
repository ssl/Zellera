package zel.room.chat

import zel.protocol.generated.FloodControlOut
import zel.protocol.generated.FloodControlOutCodec
import zel.protocol.generated.RemainingMutePeriodOut
import zel.protocol.generated.RemainingMutePeriodOutCodec
import zel.room.RoomScope
import zel.room.event.LineHiding
import zel.room.event.RoomEvent
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.util.MILLIS_PER_SECOND

@Suppress("LongParameterList") // one per chat step
class RoomChat(
    private val host: ChatHost,
    private val hooks: ChatRoomHooks,
    private val words: RoomWords,
    private val flood: FloodControl,
    private val commands: ChatCommands,
    private val gestures: Gestures,
    private val heads: HeadTurns,
    private val active: (RoomScope, RoomUnit) -> Unit,
) {
    fun say(
        scope: RoomScope,
        unit: RoomUnit,
        kind: ChatKind,
        message: String,
        requestedStyle: Int,
        trackingId: Int = ChatBubble.NO_TRACKING,
    ) {
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return
        val speaker = host.speaker(playerId) ?: return
        when (val verdict = screen(scope, unit, speaker, kind, message)) {
            Verdict.Dropped -> {}

            is Verdict.MuteLeft -> {
                remaining(scope, playerId, verdict.seconds)
            }

            is Verdict.Flooded -> {
                val tripped = FloodControlOut(verdict.muteSeconds)
                scope.out.send(Lane.Now, Audience.Player(playerId), FloodControlOutCodec, tripped)
            }

            is Verdict.Spoken -> {
                val style = ChatStyleRules.of(speaker, requestedStyle) { host.ownsStyle(playerId, it) }
                val typed = verdict.typed.text
                val line = Line(unit, playerId, kind, verdict.text, typed, style, trackingId, verdict.said)
                if (kind == ChatKind.Whisper) whisper(scope, line, verdict.typed) else talk(scope, line)
            }
        }
    }

    /** Each step sees only what the earlier ones passed. */
    @Suppress("ReturnCount") // one verdict per step
    private fun screen(
        scope: RoomScope,
        unit: RoomUnit,
        speaker: ChatSpeaker,
        kind: ChatKind,
        message: String,
    ): Verdict {
        val now = scope.now()
        val ownMute = maxOf(speaker.mutedSeconds, wholeSeconds(flood.mutedFor(speaker.playerId, speaker.login, now)))
        if (ownMute > 0) return Verdict.MuteLeft(ownMute)

        val whispered = kind == ChatKind.Whisper
        // the target name comes off first, so only the whispered text is cut, as a talk's whole line is
        val parsed = if (whispered) ChatText.whisper(message) else WhisperLine(null, message)
        val typed = parsed.copy(text = ChatText.cut(parsed.text))
        val coloured = ChatText.colourCodes(typed.text, speaker.clubMember)
        val filtered = if (speaker.skipsFilters) coloured else gameFiltered(speaker.playerId, coloured)

        active(scope, unit)
        gestures.typing(scope, unit, false)

        val rules = host.rules()
        if (filtered.isEmpty() && !whispered) return Verdict.Dropped
        if (hooks.inMuteArea(scope, unit) && !(whispered && rules.whisperInMuteArea)) return Verdict.Dropped
        val text = if (speaker.skipsFilters) filtered else words.apply(scope.room, filtered)

        val rights = hooks.hasRights(scope, speaker.playerId)
        roomSilence(scope, speaker, rights)?.let { return it }
        if (counts(scope, speaker, rights, rules) &&
            flood.trips(speaker.playerId, speaker.login, floodRuleOf(scope), now, rules.floodMuteSeconds)
        ) {
            return Verdict.Flooded(rules.floodMuteSeconds)
        }
        if (!whispered && commands.run(scope, unit, speaker.playerId, coloured)) return Verdict.Dropped
        return Verdict.Spoken(ChatText.trim(text), typed, text)
    }

    private fun roomSilence(
        scope: RoomScope,
        speaker: ChatSpeaker,
        rights: Boolean,
    ): Verdict? {
        if (speaker.skipsMutes || rights) return null
        if (hooks.mutedAll(scope)) return Verdict.Dropped
        return hooks.roomMuteSeconds(scope, speaker.playerId).takeIf { it > 0 }?.let(Verdict::MuteLeft)
    }

    private fun counts(
        scope: RoomScope,
        speaker: ChatSpeaker,
        rights: Boolean,
        rules: ChatRules,
    ): Boolean {
        if (speaker.skipsFlood) return false
        val privileged = rights || speaker.playerId == scope.room.info.ownerId
        return !privileged || rules.floodCountsRights
    }

    private fun floodRuleOf(scope: RoomScope): FloodRule = FloodRule.of(scope.room.info.moderation.floodFilter)

    private fun talk(
        scope: RoomScope,
        line: Line,
    ) {
        val hiding = LineHiding(holdsAfterLine = true)
        val shout = line.kind == ChatKind.Shout
        val says = RoomEvent.Says(line.unit.index, line.text, shout, line.style, hiding, line.said)
        scope.raise(says)
        if (says.hiding.hidden) {
            ChatOut.send(scope, ChatKind.Whisper, line.playerId, line.bubble(line.unit.index, ChatBubble.NO_TRACKING))
        } else {
            listeners(scope, line).forEach { (playerId, receiver) ->
                val tracking = if (receiver === line.unit && line.kind == ChatKind.Talk) line.trackingId else -1
                ChatOut.send(scope, line.kind, playerId, line.bubble(receiver.index, tracking, heard(line, playerId)))
            }
            if (!hooks.confinesToArea(scope, line.unit)) {
                scope.room.spectators
                    .filterNot { host.ignores(it, line.playerId) }
                    .forEach { ChatOut.send(scope, line.kind, it, line.bubble(NO_RECEIVER, -1, heard(line, it))) }
            }
            heads.heard(scope, line.unit)
            if (scope.room.units
                    .playerIds()
                    .any { it != line.playerId }
            ) {
                host.talked(line.playerId)
            }
        }
        says.hiding.lineSent(scope)
        log(scope, line, toId = 0)
    }

    private fun whisper(
        scope: RoomScope,
        line: Line,
        parsed: WhisperLine,
    ) {
        val target =
            parsed.target?.let { name ->
                scope.room.units.all().firstOrNull { unit ->
                    val id = (unit.kind as? UnitKind.Player)?.playerId
                    id != null && id != line.playerId && host.speaker(id)?.name == name
                }
            }
        val targetId = (target?.kind as? UnitKind.Player)?.playerId
        val index = target?.index ?: line.unit.index
        ChatOut.send(scope, ChatKind.Whisper, line.playerId, line.bubble(index, ChatBubble.NO_TRACKING))
        if (targetId != null && !host.ignores(targetId, line.playerId)) {
            ChatOut.send(
                scope,
                ChatKind.Whisper,
                targetId,
                line.bubble(index, ChatBubble.NO_TRACKING, heard(line, targetId)),
            )
        }
        log(scope, line, toId = targetId ?: 0)
    }

    private fun listeners(
        scope: RoomScope,
        line: Line,
    ): List<Pair<Int, RoomUnit>> =
        scope.room.units.all().mapNotNull { receiver ->
            val id = (receiver.kind as? UnitKind.Player)?.playerId ?: return@mapNotNull null
            val hears =
                receiver === line.unit ||
                    (!host.ignores(id, line.playerId) && hooks.hears(scope, line.unit, receiver))
            if (hears) id to receiver else null
        }

    private fun heard(
        line: Line,
        receiverId: Int,
    ): String = if (receiverId == line.playerId) line.text else host.heardText(receiverId, line.text)

    private fun gameFiltered(
        playerId: Int,
        text: String,
    ): String =
        when (val filtered = host.filter(playerId, text)) {
            is ChatFilterResult.Say -> filtered.text
            ChatFilterResult.Hide -> ""
        }

    private fun log(
        scope: RoomScope,
        line: Line,
        toId: Int,
    ) = host.log(ChatLogLine(scope.room.info.id, line.playerId, toId, ChatText.trim(line.unfiltered)))

    private fun remaining(
        scope: RoomScope,
        playerId: Int,
        seconds: Int,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        RemainingMutePeriodOutCodec,
        RemainingMutePeriodOut(seconds.coerceAtLeast(1)),
    )

    private fun wholeSeconds(millis: Long): Int = ((millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()

    private class Line(
        val unit: RoomUnit,
        val playerId: Int,
        val kind: ChatKind,
        val text: String,
        val unfiltered: String,
        val style: Int,
        val trackingId: Int,
        val said: String = text,
    ) {
        fun bubble(
            receiverIndex: Int,
            trackingId: Int,
            heard: String = text,
        ) = ChatBubble(unit.index, heard, ChatText.gesture(text), style, trackingId, receiverIndex)
    }

    private sealed interface Verdict {
        data object Dropped : Verdict

        data class MuteLeft(
            val seconds: Int,
        ) : Verdict

        data class Flooded(
            val muteSeconds: Int,
        ) : Verdict

        data class Spoken(
            val text: String,
            val typed: WhisperLine,
            /** The line before its trailing spaces were dropped: what a says trigger's capture reads. */
            val said: String = text,
        ) : Verdict
    }

    private companion object {
        /** A spectator has no avatar the bubble could be addressed to. */
        const val NO_RECEIVER = -1
    }
}
