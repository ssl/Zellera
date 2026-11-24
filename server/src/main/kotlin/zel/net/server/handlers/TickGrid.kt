package zel.net.server.handlers

/** A shared 30 s grid, so sockets opened seconds apart are checked together. */
internal object TickGrid {
    const val STEP_MILLIS = 30_000L

    /** A check this close to a grid point counts as that point and the next one is taken. */
    const val EARLY_MILLIS = 1_000L

    fun untilNext(nowMillis: Long): Long {
        val next = STEP_MILLIS - Math.floorMod(nowMillis, STEP_MILLIS)
        return if (next < EARLY_MILLIS) next + STEP_MILLIS else next
    }
}
