package zel.wired.effect.bot

import zel.game.bots.BotWiredTalkLimit
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.wired.effect.user.TextPlaceholders
import zel.wired.firing.Firing
import zel.wired.timing.RoomPoster
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class BotTalks(
    private val poster: RoomPoster,
    val texts: TextPlaceholders,
    private val random: Random = Random.Default,
) : RoomPart {
    private val limits = ConcurrentHashMap<Room, MutableMap<Int, BotWiredTalkLimit>>()

    /** The bot lines of each firing still to say, by room; each posted job says one of them, drawn at random. */
    private val batches = ConcurrentHashMap<Room, IdentityHashMap<Firing, MutableList<Line>>>()

    private class Line(
        val botId: Int,
        val say: (RoomScope) -> Unit,
    )

    override fun load(room: Room) {
        limits[room] = HashMap()
    }

    override fun unload(scope: RoomScope) {
        limits.remove(scope.room)
        batches.remove(scope.room)
    }

    fun later(
        firing: Firing,
        botId: Int,
        say: (RoomScope) -> Unit,
    ) {
        val room = firing.scope.room
        // The lines of one firing come out in a new order each time.
        batches.getOrPut(room) { IdentityHashMap() }.getOrPut(firing) { ArrayList() } += Line(botId, say)
        poster.post(room.info.id, 0) { scope ->
            if (scope.room === room) next(room, firing)?.let { line -> if (allowed(scope, line.botId)) line.say(scope) }
        }
    }

    private fun next(
        room: Room,
        firing: Firing,
    ): Line? {
        val byFiring = batches[room] ?: return null
        val lines = byFiring[firing] ?: return null
        val line = lines.removeAt(random.nextInt(lines.size))
        if (lines.isEmpty()) byFiring.remove(firing)
        return line
    }

    private fun allowed(
        scope: RoomScope,
        botId: Int,
    ): Boolean =
        limits
            .getOrPut(scope.room) { HashMap() }
            .getOrPut(botId) { BotWiredTalkLimit() }
            .allow(scope.now())
}
