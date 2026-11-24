package zel.game.guilds.forums

data class MessageData(
    val messageId: Int,
    val index: Int,
    val authorId: Int,
    val authorName: String,
    val authorLook: String,
    val ageSeconds: Int,
    val text: String,
    val stateId: Byte,
    val adminId: Int,
    val adminName: String,
    val adminOperationValue: Int,
    val authorPostCount: Int,
)
