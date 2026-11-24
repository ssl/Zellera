package zel.room.furni

import kotlin.math.roundToInt

data class StackHelperState(
    val heightText: String,
    val multiWalk: Boolean,
) {
    val stored: String get() = if (multiWalk) "$heightText$MULTI_WALK" else heightText

    fun at(height: Double): String = StackHelperState(hundredths(height), multiWalk).stored

    companion object {
        private const val MULTI_WALK = ";1"
        private const val SEPARATOR = ';'
        private const val HUNDRED = 100.0

        fun of(state: String?): StackHelperState {
            val text = state.orEmpty()
            val tick = SEPARATOR in text && text.substringAfter(SEPARATOR) == "1"
            return StackHelperState(text.substringBefore(SEPARATOR), tick)
        }

        fun stored(
            height: Double,
            multiWalk: Boolean,
        ): String = StackHelperState(hundredths(height), multiWalk).stored

        private fun hundredths(height: Double): String = (height * HUNDRED).roundToInt().toString()
    }
}
