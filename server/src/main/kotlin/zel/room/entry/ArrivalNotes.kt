package zel.room.entry

import java.util.concurrent.ConcurrentHashMap

class ArrivalNotes {
    private val notes = ConcurrentHashMap<Int, Note>()

    fun note(
        playerId: Int,
        roomId: Int,
        furniId: Int,
        nowMillis: Long,
        skipsDoor: Boolean = false,
        effect: Int = 0,
        method: Int = RoomEntryMethod.DEFAULT,
    ) {
        notes[playerId] = Note(roomId, furniId, nowMillis, skipsDoor, effect, method)
    }

    fun take(
        playerId: Int,
        roomId: Int,
        nowMillis: Long,
    ): Arrival? {
        val note = notes.remove(playerId) ?: return null
        val fresh = nowMillis - note.atMillis <= VALID_MILLIS
        if (note.roomId != roomId || !fresh) return null
        return Arrival(note.furniId, note.skipsDoor, note.effect, note.method)
    }

    /**
     * [effect]: shown on the arriving user at the end of the entry burst, then their own again; 0 for none.
     * [method]: how the user came in, read as `@room_entry.method` ([RoomEntryMethod]).
     */
    data class Arrival(
        val furniId: Int,
        val skipsDoor: Boolean,
        val effect: Int = 0,
        val method: Int = RoomEntryMethod.DEFAULT,
    )

    private data class Note(
        val roomId: Int,
        val furniId: Int,
        val atMillis: Long,
        val skipsDoor: Boolean,
        val effect: Int,
        val method: Int,
    )

    companion object {
        const val VALID_MILLIS = 15_000L
    }
}
