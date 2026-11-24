package zel.room.rights

sealed interface SettingsAnswer {
    data class Saved(
        val settings: RoomSettings,
    ) : SettingsAnswer

    data class Refused(
        val code: Int,
        val text: String,
    ) : SettingsAnswer

    /** A value outside the protocol: no answer, nothing stored. */
    data object Ignored : SettingsAnswer
}
