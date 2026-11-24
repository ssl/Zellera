package zel.room.chat

data class ChatBubbleStyle(
    val type: Int,
    val name: String,
    val permission: String,
    val overridable: Boolean,
    val triggersTalkingFurniture: Boolean,
)
