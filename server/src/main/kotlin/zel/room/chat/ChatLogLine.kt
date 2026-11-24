package zel.room.chat

data class ChatLogLine(
    val roomId: Int,
    val fromId: Int,
    val toId: Int,
    val text: String,
)
