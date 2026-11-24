package zel.room.tick

import zel.protocol.generated.OutgoingCodec
import zel.room.Room

class TickOutput(
    private val room: Room,
    private val channels: PlayerChannels,
) : RoomOutput {
    private var inTick = false
    private var batchDepth = 0
    private val touched = LinkedHashSet<Int>()
    private val afterUnitUpdate = ArrayList<Held<*>>()
    private val endOfTick = ArrayList<Held<*>>()
    private val nextTick = ArrayList<Held<*>>()
    private val nextTickAfterUnitUpdate = ArrayList<Held<*>>()

    override fun <T> send(
        lane: Lane,
        to: Audience,
        codec: OutgoingCodec<T>,
        message: T,
    ) {
        checkLocked()
        val players = recipients(to)
        if (players.isEmpty()) return
        val parts = if (room.sight.active) room.sight.split(players, codec, message) else null
        if (parts != null) {
            parts.forEach { (playerId, part) -> hold(lane, Held(listOf(playerId), codec, part)) }
            return
        }
        hold(lane, Held(players, codec, message))
    }

    private fun hold(
        lane: Lane,
        held: Held<*>,
    ) {
        when (lane) {
            Lane.Now -> write(held)
            Lane.AfterUnitUpdate -> if (inTick) afterUnitUpdate += held else write(held)
            Lane.EndOfTick -> if (inTick) endOfTick += held else write(held)
            Lane.NextTick -> nextTick += held
            Lane.NextTickAfterUnitUpdate -> nextTickAfterUnitUpdate += held
        }
    }

    fun beginTick() {
        checkLocked()
        check(!inTick) { "room ${room.info.id}: tick started twice" }
        inTick = true
    }

    /** What the last tick held back; what waits for this tick's RoomUnitUpdate joins that lane. */
    fun sendLeftovers() {
        drain(nextTick)
        afterUnitUpdate += nextTickAfterUnitUpdate
        nextTickAfterUnitUpdate.clear()
    }

    fun sendAfterUnitUpdate() = drain(afterUnitUpdate)

    fun sendEndOfTick() = drain(endOfTick)

    fun endTick() {
        checkLocked()
        try {
            sendAfterUnitUpdate()
            sendEndOfTick()
        } finally {
            inTick = false
            flushTouched()
        }
    }

    fun beginBatch() {
        checkLocked()
        batchDepth++
    }

    fun endBatch() {
        checkLocked()
        check(batchDepth > 0) { "room ${room.info.id}: batch ended twice" }
        batchDepth--
        if (batchDepth == 0 && !inTick) flushTouched()
    }

    fun dropHeld() {
        checkLocked()
        afterUnitUpdate.clear()
        endOfTick.clear()
        nextTick.clear()
        nextTickAfterUnitUpdate.clear()
    }

    private fun drain(queue: MutableList<Held<*>>) {
        checkLocked()
        val due = queue.toList()
        queue.clear()
        due.forEach(::write)
    }

    private fun write(held: Held<*>) {
        held.writeTo(channels)
        touched += held.players
        if (!inTick && batchDepth == 0) flushTouched()
    }

    private fun flushTouched() {
        if (touched.isEmpty()) return
        val players = touched.toList()
        touched.clear()
        channels.flush(players)
    }

    private fun recipients(to: Audience): List<Int> =
        when (to) {
            Audience.Room -> room.units.playerIds() + room.spectators
            is Audience.Player -> listOf(to.playerId)
            is Audience.RoomExcept -> (room.units.playerIds() + room.spectators).filter { it != to.playerId }
        }.filterNot(room.units::isLeaving)

    private fun checkLocked() = check(room.isLockedHere) { "room ${room.info.id}: output used outside the room lock" }

    private class Held<T>(
        val players: List<Int>,
        val codec: OutgoingCodec<T>,
        val message: T,
    ) {
        fun writeTo(channels: PlayerChannels) = channels.write(players, codec, message)
    }
}
