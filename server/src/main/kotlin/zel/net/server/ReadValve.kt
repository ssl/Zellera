package zel.net.server

/**
 * Pauses a connection's reads at [share] packets or [shareBytes] body bytes in flight, or when the pool is full;
 * resumes once its packets are done and the pool has room. While the pool is [heavy], only connections over
 * [lightBytes] pause. Socket thread only.
 */
internal class ReadValve(
    private val share: Int,
    private val pauses: Boolean,
    private val shareBytes: Long = Long.MAX_VALUE,
    private val lightBytes: Long = Long.MAX_VALUE,
    private val heavy: () -> Boolean = { false },
    private val full: () -> Boolean,
) {
    enum class Move {
        NOTHING,
        SHUT,
        OPEN,
        WAIT_FOR_ROOM,
    }

    private var inPool = 0
    private var bytesInPool = 0L
    private var open = true
    private var waitingForRoom = false

    fun handedOver(bytes: Int = 0): Move {
        inPool++
        bytesInPool += bytes
        if (!pauses || !open || !overShare()) return Move.NOTHING
        open = false
        return Move.SHUT
    }

    fun cameBack(bytes: Int = 0): Move {
        inPool--
        bytesInPool -= bytes
        return reconsider()
    }

    fun poolHasRoom(): Move {
        waitingForRoom = false
        return reconsider()
    }

    private fun overShare(): Boolean =
        inPool >= share || bytesInPool >= shareBytes || full() || (bytesInPool > lightBytes && heavy())

    private fun reconsider(): Move =
        when {
            open || inPool > 0 -> {
                Move.NOTHING
            }

            !full() && !heavy() -> {
                open = true
                Move.OPEN
            }

            waitingForRoom -> {
                Move.NOTHING
            }

            else -> {
                waitingForRoom = true
                Move.WAIT_FOR_ROOM
            }
        }
}
