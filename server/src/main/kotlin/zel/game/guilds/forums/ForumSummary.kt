package zel.game.guilds.forums

data class ForumSummary(
    val guildId: Int,
    val name: String,
    val description: String,
    val badge: String,
    val totalThreads: Int,
    val totalComments: Int,
    val newComments: Int,
    val lastCommentId: Int,
    val lastAuthorId: Int,
    val lastAuthorName: String,
    val lastCommentAgeSeconds: Int,
)
