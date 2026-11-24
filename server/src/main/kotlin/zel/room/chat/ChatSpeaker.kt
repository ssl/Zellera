package zel.room.chat

data class ChatSpeaker(
    val playerId: Int,
    val login: Any,
    val name: String,
    val clubMember: Boolean,
    val mutedSeconds: Int = 0,
    val skipsFilters: Boolean = false,
    val skipsFlood: Boolean = false,
    val skipsMutes: Boolean = false,
    val anyStyle: Boolean = false,
    val ambassador: Boolean = false,
)
