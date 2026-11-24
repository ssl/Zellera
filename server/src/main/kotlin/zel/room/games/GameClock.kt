package zel.room.games

import zel.util.MILLIS_PER_SECOND

class GameClock(
    val kind: ClockKind,
    val steps: IntArray,
) {
    var baseSeconds: Int = 0
    var seconds: Int = 0
    var halfSecond: Boolean = false
    var running: Boolean = false
    var paused: Boolean = false

    var ticking: Boolean = false

    var chain: Int = 0

    /** An adjust clock wrote the time while the clock stood still; the next start counts on from it. */
    var setWhileStopped: Boolean = false

    var shownState: String? = null

    val millis: Int get() = seconds * SECOND_MILLIS + if (halfSecond) HALF_SECOND else 0

    val maxMillis: Int get() = baseSeconds * SECOND_MILLIS

    /** Sets a count-up clock's time, cut to its range and to whole half seconds. */
    fun setMillis(total: Int) {
        val clamped = total.coerceIn(0, maxMillis)
        seconds = clamped / SECOND_MILLIS
        halfSecond = clamped % SECOND_MILLIS >= HALF_SECOND
    }

    val nextCountMillis: Int get() = if (halfSecond) HALF_SECOND else SECOND_MILLIS

    fun shown(): String =
        if (kind == ClockKind.Hourglass) {
            when {
                running && !paused -> RUNNING_STATE
                millis > 0 -> PAUSED_STATE
                else -> STOPPED_STATE
            }
        } else {
            seconds.toString()
        }

    fun stored(): String = "$seconds\t$baseSeconds"

    companion object {
        const val HALF_SECOND = 500
        private const val SECOND_MILLIS = MILLIS_PER_SECOND.toInt()

        const val COUNTER_MAX_SECONDS = 99 * 60 + 59

        private const val STOPPED_STATE = "0"
        private const val RUNNING_STATE = "1"
        private const val PAUSED_STATE = "2"

        private val DEFAULT_STEPS = intArrayOf(30, 60, 120, 180, 300, 600)

        fun load(
            kind: ClockKind,
            customParams: String,
            state: String,
        ): GameClock {
            val fields = state.split('\t')
            return if (kind.countsUp) {
                GameClock(kind, intArrayOf(COUNTER_MAX_SECONDS)).apply {
                    baseSeconds = COUNTER_MAX_SECONDS
                    setMillis((fields[0].toIntOrNull() ?: 0).coerceAtLeast(0) * SECOND_MILLIS)
                }
            } else {
                val steps =
                    if (customParams.isEmpty()) {
                        DEFAULT_STEPS
                    } else {
                        customParams
                            .split(',')
                            .map {
                                it.toIntOrNull()
                                    ?: 0
                            }.toIntArray()
                    }
                GameClock(kind, steps).apply {
                    baseSeconds =
                        when {
                            fields.size >= 2 -> fields[1].toIntOrNull() ?: steps[0]
                            fields[0].toIntOrNull() != null -> 0
                            else -> steps[0]
                        }
                    seconds = baseSeconds
                }
            }
        }
    }
}
