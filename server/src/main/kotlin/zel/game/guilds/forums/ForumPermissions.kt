package zel.game.guilds.forums

data class ForumPermissions(
    val readLevel: Int,
    val postMessagesLevel: Int,
    val postThreadsLevel: Int,
    val moderateLevel: Int,
    val readError: String,
    val postError: String,
    val startThreadError: String,
    val moderateError: String,
    val canChangeSettings: Boolean,
    val canModerate: Boolean,
)
