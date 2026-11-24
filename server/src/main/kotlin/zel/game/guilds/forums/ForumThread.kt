package zel.game.guilds.forums

/** Posts load on first use and are guarded by the thread's lock; other fields are volatile. */
@Suppress("LongParameterList") // one guild_forum_threads row
class ForumThread(
    val id: Int,
    val guildId: Int,
    val openerId: Int,
    val subject: String,
    val createdAt: Int,
    private val loadPosts: (Int) -> List<ForumPost>,
    postCount: Int = 0,
    updatedAt: Int = createdAt,
    flags: Flags = Flags(),
    lastPost: ForumPost? = null,
) {
    data class Flags(
        val state: ForumState = ForumState.OPEN,
        val pinned: Boolean = false,
        val locked: Boolean = false,
        val moderation: ForumModeration = ForumModeration(),
    )

    // reads and writes happen under the thread's lock, so the lazy needs no lock of its own
    private val loaded: MutableList<ForumPost> by lazy(LazyThreadSafetyMode.NONE) {
        loadPosts(id).toMutableList().also { posts -> posts.forEachIndexed { index, post -> post.position = index } }
    }

    @Volatile var postCount: Int = postCount
        private set

    @Volatile var updatedAt: Int = updatedAt
        private set

    @Volatile var flags: Flags = flags
        private set

    @Volatile var lastPost: ForumPost? = lastPost
        private set

    @Volatile var hasUnsavedChanges: Boolean = false
        private set

    val state: ForumState get() = flags.state
    val isPinned: Boolean get() = flags.pinned
    val isLocked: Boolean get() = flags.locked

    fun posts(): List<ForumPost> = synchronized(this) { loadedPosts().toList() }

    fun posts(
        limit: Int,
        offset: Int,
    ): List<ForumPost> = synchronized(this) { loadedPosts().drop(offset.coerceAtLeast(0)).take(limit.coerceAtLeast(0)) }

    fun post(postId: Int): ForumPost? = synchronized(this) { loadedPosts().firstOrNull { it.id == postId } }

    /** Adds a new post at the end; a post the first read already brought takes that one's place. */
    fun add(post: ForumPost) {
        synchronized(this) {
            val posts = loadedPosts()
            val known = posts.indexOfFirst { it.id == post.id }
            if (known >= 0) {
                post.position = known
            } else {
                post.position = posts.size
                posts.add(post)
            }
            if (post.id >= (lastPost?.id ?: 0)) lastPost = post
        }
    }

    /** A stored post joins the thread: the row already counts it. */
    fun posted(
        post: ForumPost,
        now: Int,
    ) {
        synchronized(this) {
            add(post)
            postCount++
            updatedAt = now
        }
    }

    fun setPinnedAndLocked(
        pinned: Boolean,
        locked: Boolean,
    ) {
        flags = flags.copy(pinned = pinned, locked = locked)
        hasUnsavedChanges = true
    }

    fun moderate(
        state: ForumState,
        by: ForumModeration,
    ) {
        flags = flags.copy(state = state, moderation = by)
        hasUnsavedChanges = true
    }

    fun saved() {
        hasUnsavedChanges = false
    }

    private fun loadedPosts(): MutableList<ForumPost> = loaded

    override fun equals(other: Any?): Boolean = other is ForumThread && other.id == id

    override fun hashCode(): Int = id
}
