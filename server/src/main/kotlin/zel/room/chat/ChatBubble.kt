package zel.room.chat

data class ChatBubble(
    val unitIndex: Int,
    val text: String,
    val gesture: Int,
    val style: Int,
    val trackingId: Int,
    val receiverIndex: Int,
    val width: Int = USER_WIDTH,
    val hideName: Boolean? = null,
) {
    companion object {
        const val USER_WIDTH = -1
        const val NO_TRACKING = -1
    }
}
