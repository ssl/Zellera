package zel.room.life

import zel.core.rules.rules

data class LifeTimes(
    val sleepTicks: Int = rules.sleepSeconds * LifeTiming.TICKS_PER_SECOND,
    /** False: a user only idles in a room with its own sleep or kick time. */
    val autoIdle: Boolean = rules.autoSleep,
    val decoHosting: Boolean = true,
)
