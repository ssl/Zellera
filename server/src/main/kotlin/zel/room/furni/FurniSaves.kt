package zel.room.furni

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.room.Room
import zel.room.RoomWrites
import zel.room.persist.FurniData
import zel.room.persist.FurniSave
import java.util.concurrent.atomic.AtomicLong

class FurniSaves(
    private val data: FurniData,
    private val writes: RoomWrites,
) {
    private val handed = AtomicLong()

    fun handedIn(): Long = handed.get()

    /** The guarded update skips a row that left the room first. */
    fun saveDirty(room: Room): Int {
        check(room.isLockedHere) { "room ${room.info.id}: furni saved outside the room lock" }
        val changes = room.furni.takeChanged().map(RoomFurni::toSave)
        if (changes.isEmpty()) return 0
        val roomId = room.info.id
        writes.write(roomId, Runnable { write(roomId, changes) })
        handed.addAndGet(changes.size.toLong())
        return changes.size
    }

    @Suppress("TooGenericExceptionCaught") // logged; the room keeps running
    private fun write(
        roomId: Int,
        changes: List<FurniSave>,
    ) {
        try {
            data.save(roomId, changes)
        } catch (e: Exception) {
            if (changes.size == 1) return log.error("room {}: saving furni {} failed", roomId, changes[0].id, e)
            log.warn("room {}: saving {} furni in one batch failed, saving them one by one", roomId, changes.size, e)
            changes.forEach { write(roomId, listOf(it)) }
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(FurniSaves::class.java)
    }
}

fun RoomFurni.toSave(): FurniSave {
    val anchor = anchor
    return FurniSave(
        id = rowId,
        wallPosition = wallPosition.orEmpty(),
        x = anchor?.x ?: 0,
        y = anchor?.y ?: 0,
        z = z,
        direction = direction,
        state = state,
    )
}
