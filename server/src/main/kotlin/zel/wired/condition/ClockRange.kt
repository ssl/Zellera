package zel.wired.condition

internal object ClockRange {
    fun contains(
        value: Int,
        min: Int,
        max: Int,
    ): Boolean =
        when {
            min == max -> value == min
            min < max -> value in min..max
            else -> value >= min || value <= max
        }
}
