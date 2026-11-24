package zel.game.guilds.forums

data class ThreadData(
    val threadId: Int,
    val openerId: Int,
    val openerName: String,
    val subject: String,
    val pinned: Boolean,
    val locked: Boolean,
    val ageSeconds: Int,
    val totalComments: Int,
    val newComments: Int,
    val lastMessageId: Int,
    val lastAuthorId: Int,
    val lastAuthorName: String,
    val lastMessageAgeSeconds: Int,
    val stateId: Byte,
    val adminId: Int,
    val adminName: String,
    val adminOperationValue: Int,
)
