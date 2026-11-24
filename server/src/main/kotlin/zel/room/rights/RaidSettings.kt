package zel.room.rights

data class RaidSettings(
    val enabled: Boolean,
    val sensitivity: Int,
    val actionType: Int,
    val banSeconds: Int,
    val guardEnabled: Boolean,
    val guardSeconds: Int,
    val guardSensitivity: Int,
) {
    fun valid(): Boolean =
        sensitivity in SENSITIVITIES &&
            actionType in ACTION_TYPES &&
            banSeconds in BAN_SECONDS &&
            guardSeconds in GUARD_SECONDS &&
            guardSensitivity in SENSITIVITIES

    companion object {
        private const val DEFAULT_SECONDS = 900
        private val SENSITIVITIES = 0..2
        private val ACTION_TYPES = 0..1

        @Suppress("MagicNumber") // the protocol's ban durations, in seconds
        private val BAN_SECONDS = setOf(300, 900, 1800, 3600, 10800, 21600, 43200, 86400, 259200, 604800)

        @Suppress("MagicNumber") // the protocol's guard durations, in seconds
        private val GUARD_SECONDS = setOf(300, 900, 1800, 3600, 10800)

        val DEFAULT =
            RaidSettings(
                enabled = false,
                sensitivity = 1,
                actionType = 1,
                banSeconds = DEFAULT_SECONDS,
                guardEnabled = false,
                guardSeconds = DEFAULT_SECONDS,
                guardSensitivity = 1,
            )
    }
}
