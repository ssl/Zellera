package zel.room.side

import zel.protocol.generated.QuestionAnsweredOut
import zel.protocol.generated.QuestionAnsweredOutCodec
import zel.protocol.generated.QuestionFinishedOut
import zel.protocol.generated.QuestionFinishedOutCodec
import zel.protocol.generated.QuestionOut
import zel.protocol.generated.QuestionOutCodec
import zel.room.Room
import zel.room.RoomListener
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.tick.TickPart
import zel.room.unit.UnitKind
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class RoomQuestions :
    RoomPart,
    RoomListener {
    /** The question a room is asking; votes by player id, "0" dislike or "1" like. */
    class Asked(
        val id: Int,
        val text: String,
        val endsAt: Long,
    ) {
        val votes = LinkedHashMap<Int, String>()
    }

    private val asked = ConcurrentHashMap<Room, Asked>()

    private val ids = AtomicInteger()

    val tick: TickPart =
        TickPart { scope ->
            val question = asked[scope.room] ?: return@TickPart
            if (scope.now() < question.endsAt) return@TickPart
            asked.remove(scope.room)
            val result = counts(question).map { QuestionFinishedOut.List1Item(it.string1, it.int1) }
            val finished = QuestionFinishedOut(question.id, result)
            scope.out.send(Lane.Now, Audience.Room, QuestionFinishedOutCodec, finished)
        }

    override fun unload(scope: RoomScope) {
        asked.remove(scope.room)
    }

    override fun on(
        scope: RoomScope,
        event: RoomEvent,
    ) {
        if (event !is RoomEvent.Entered) return
        val question = asked[scope.room] ?: return
        val playerId = (scope.room.units[event.unitIndex]?.kind as? UnitKind.Player)?.playerId ?: return
        scope.out.send(Lane.Now, Audience.Player(playerId), QuestionOutCodec, shown(question, scope.now()))
    }

    /** Puts [text] to the room for [millis]; false while another question runs. */
    fun start(
        scope: RoomScope,
        text: String,
        millis: Long,
    ): Boolean {
        if (asked.containsKey(scope.room) || text.isBlank() || millis <= 0) return false
        val question = Asked(ids.incrementAndGet(), text.trim(), scope.now() + millis)
        asked[scope.room] = question
        scope.out.send(Lane.Now, Audience.Room, QuestionOutCodec, shown(question, scope.now()))
        return true
    }

    fun answer(
        scope: RoomScope,
        playerId: Int,
        questionId: Int,
        value: String,
    ) {
        val question = asked[scope.room]?.takeIf { it.id == questionId } ?: return
        if (value !in VALUES || playerId in question.votes || scope.room.units.ofPlayer(playerId) == null) return
        question.votes[playerId] = value
        val answered = QuestionAnsweredOut(playerId, value, counts(question))
        scope.out.send(Lane.Now, Audience.Room, QuestionAnsweredOutCodec, answered)
    }

    private fun shown(
        question: Asked,
        now: Long,
    ): QuestionOut =
        QuestionOut(
            question.text,
            NO_POLL,
            NO_POLL,
            (question.endsAt - now).coerceAtLeast(0).toInt(),
            question.id,
            FIRST,
            LIKE_DISLIKE,
            question.text,
            QuestionOut.Branch1.Else,
        )

    private fun counts(question: Asked): List<QuestionAnsweredOut.List1Item> =
        VALUES.map { value -> QuestionAnsweredOut.List1Item(value, question.votes.values.count { it == value }) }

    private companion object {
        val VALUES = listOf("0", "1")
        const val NO_POLL = 0
        const val FIRST = 0
        const val LIKE_DISLIKE = 3
    }
}
