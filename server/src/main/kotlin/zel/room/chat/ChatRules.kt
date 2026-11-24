package zel.room.chat

data class ChatRules(
    val floodMuteSeconds: Int = FloodControl.MUTE_SECONDS,
    val floodCountsRights: Boolean = false,
    val whisperInMuteArea: Boolean = false,
)
