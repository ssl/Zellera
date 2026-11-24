package zel.game.guilds.forums

class ForumPost(
    val id: Int,
    val threadId: Int,
    val authorId: Int,
    val message: String,
    val createdAt: Int,
    state: ForumState = ForumState.OPEN,
    moderation: ForumModeration = ForumModeration(),
) {
    @Volatile
    var position = UNLISTED

    @Volatile
    var state: ForumState = state
        private set

    @Volatile
    var moderation: ForumModeration = moderation
        private set

    @Volatile
    var hasUnsavedChanges = false
        private set

    fun moderate(
        changed: ForumState,
        by: ForumModeration,
    ) {
        state = changed
        moderation = by
        hasUnsavedChanges = true
    }

    fun saved() {
        hasUnsavedChanges = false
    }

    private companion object {
        const val UNLISTED = -1
    }
}
