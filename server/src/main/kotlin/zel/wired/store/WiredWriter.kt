package zel.wired.store

import zel.room.OrderedRoomWrites

/** Runs a room's wired writes after all its earlier work, so a later save never lands first. */
fun interface WiredWriter {
    fun write(
        roomId: Int,
        job: Runnable,
    )

    /** Held until the room's next hand-over; a job under the same [key] replaces it. */
    fun writeLatest(
        roomId: Int,
        key: Any,
        job: Runnable,
    ) = write(roomId, job)

    fun flush(roomId: Int) = Unit

    companion object {
        /** A room load that waits for its writes also waits for its wired. */
        fun of(writes: OrderedRoomWrites) =
            object : WiredWriter {
                override fun write(
                    roomId: Int,
                    job: Runnable,
                ) = writes.write(roomId, job)

                override fun writeLatest(
                    roomId: Int,
                    key: Any,
                    job: Runnable,
                ) = writes.writeLatest(roomId, key, job)

                override fun flush(roomId: Int) = writes.flush(roomId)
            }

        val Inline = WiredWriter { _, job -> job.run() }
    }
}
