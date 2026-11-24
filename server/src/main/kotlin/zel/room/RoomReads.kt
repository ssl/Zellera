package zel.room

import org.slf4j.LoggerFactory

/** Reads made off the room's lock, the answer handed back on the room's thread. */
interface RoomReads {
    /** Runs [read] off the lock and [then] under it; a read that fails answers null. */
    fun <T> read(
        scope: RoomScope,
        read: () -> T?,
        then: (RoomScope, T?) -> Unit,
    )

    companion object {
        /** Reads at once, under the lock the caller already holds. */
        val Inline: RoomReads =
            object : RoomReads {
                override fun <T> read(
                    scope: RoomScope,
                    read: () -> T?,
                    then: (RoomScope, T?) -> Unit,
                ) = then(scope, read())
            }

        fun of(
            persist: (Runnable) -> Unit,
            post: (Int, (RoomScope) -> Unit) -> Unit,
        ): RoomReads =
            object : RoomReads {
                @Suppress("TooGenericExceptionCaught") // a failed read still answers
                override fun <T> read(
                    scope: RoomScope,
                    read: () -> T?,
                    then: (RoomScope, T?) -> Unit,
                ) {
                    val roomId = scope.room.info.id
                    persist(
                        Runnable {
                            val answer =
                                try {
                                    read()
                                } catch (e: Exception) {
                                    log.error("room {}: a read for the running room failed", roomId, e)
                                    null
                                }
                            post(roomId) { inner -> then(inner, answer) }
                        },
                    )
                }
            }

        private val log = LoggerFactory.getLogger(RoomReads::class.java)
    }
}
