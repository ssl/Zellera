package zel.room.furni.behaviour.state

import zel.room.RoomListener
import zel.room.RoomScope
import zel.room.chat.ChatKind
import zel.room.event.PlayerAction
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.bot.UnitHost
import zel.room.unit.bot.UnitSpeech
import kotlin.math.abs
import kotlin.math.max

class FurniListeners(
    private val part: StatePart,
    private val votes: VoteCounter,
    private val units: UnitHost,
) : RoomListener {
    override fun on(
        scope: RoomScope,
        event: RoomEvent,
    ) {
        when (event) {
            is RoomEvent.Action -> if (event.action == PlayerAction.SIGN) signed(scope, event)
            is RoomEvent.Says -> scope.room.units[event.unitIndex]?.let { answer(scope, it) }
            else -> Unit
        }
    }

    private fun signed(
        scope: RoomScope,
        event: RoomEvent.Action,
    ) {
        val playerId = (scope.room.units[event.unitIndex]?.kind as? UnitKind.Player)?.playerId ?: return
        val sign = event.param.toIntOrNull() ?: return
        scope.room.furni
            .all()
            .filter { it.behaviour === votes }
            .forEach { votes.signed(scope, it, playerId, sign) }
    }

    private fun answer(
        scope: RoomScope,
        speaker: RoomUnit,
    ) {
        if (speaker.kind !is UnitKind.Player) return
        val range = units.config(RANGE_KEY, DEFAULT_RANGE)
        val furni =
            scope.room.furni
                .all()
                .firstOrNull { talks(it) && near(it, speaker, range) } ?: return
        val name = furni.definition.name
        val lines = linesOf(name)
        if (lines.isEmpty()) return
        val line = lines[part.random.nextInt(lines.size)]
        val style = units.config("furni.talking.$name.bubble_style", PARROT_STYLE)
        part.later(scope, furni, ANSWER_MILLIS) { later, talker ->
            if (later.room.units[speaker.index] !== speaker) return@later
            UnitSpeech.toRoom(later, speaker, ChatKind.Talk, line, style)
            StateShow.data(later, talker, TALKING)
            part.later(later, talker, TALK_MILLIS) { quiet, done -> StateShow.data(quiet, done, QUIET) }
        }
    }

    private fun linesOf(name: String): List<String> =
        (0 until MAX_LINES)
            .asSequence()
            .map { units.text("$LINE_PREFIX$name.$it", "") }
            .takeWhile { it.isNotEmpty() }
            .toList()

    private fun talks(furni: RoomFurni): Boolean = Interactions.of(furni.definition) == TYPE && furni.state != TALKING

    private fun near(
        furni: RoomFurni,
        unit: RoomUnit,
        range: Int,
    ): Boolean {
        val tile = furni.anchor ?: return false
        return max(abs(tile.x - unit.tile.x), abs(tile.y - unit.tile.y)) <= range
    }

    companion object {
        const val TYPE = "state.listener"
        private const val LINE_PREFIX = "room.furni.talking."
        private const val RANGE_KEY = "furni.talking.hearing.tiles"
        private const val DEFAULT_RANGE = 1
        private const val PARROT_STYLE = 28
        private const val TALKING = "1"
        private const val QUIET = "0"
        private const val ANSWER_MILLIS = 500L
        private const val TALK_MILLIS = 1900L
        private const val MAX_LINES = 100
    }
}
