package zel.game.guilds

/** Edits mark it unsaved; fields are volatile as handlers on several threads share it. */
@Suppress("LongParameterList") // one guilds row
class Guild(
    val id: Int,
    val ownerId: Int,
    ownerName: String,
    val roomId: Int,
    val roomName: String,
    val createdAt: Int,
    face: Face,
    joining: Joining,
    forumRules: ForumRules,
    hasForum: Boolean,
) {
    data class Face(
        val name: String,
        val description: String,
        val badge: String,
        val colorOne: Int,
        val colorTwo: Int,
    )

    data class Joining(
        val state: JoinMode,
        val membersDecorate: Boolean,
    )

    @Volatile var ownerName: String = ownerName

    @Volatile var face: Face = face
        private set

    @Volatile var joining: Joining = joining
        private set

    @Volatile var forumRules: ForumRules = forumRules
        private set

    @Volatile var hasForum: Boolean = hasForum
        private set

    @Volatile var hasUnsavedChanges: Boolean = false
        private set

    @Volatile var memberCount: Int = 0
        private set

    @Volatile var requestCount: Int = 0
        private set

    val name: String get() = face.name
    val description: String get() = face.description
    val badge: String get() = face.badge
    val colorOne: Int get() = face.colorOne
    val colorTwo: Int get() = face.colorTwo
    val state: JoinMode get() = joining.state

    val rights: Boolean get() = joining.membersDecorate

    fun show(changed: Face) = edit { face = changed }

    fun admit(changed: Joining) = edit { joining = changed }

    fun ruleForum(rules: ForumRules) = edit { forumRules = rules }

    fun openForum() =
        edit {
            hasForum = true
            forumRules = ForumRules(ForumLevel.EVERYONE, ForumLevel.MEMBERS, ForumLevel.MEMBERS, ForumLevel.ADMINS)
        }

    fun saved() {
        hasUnsavedChanges = false
    }

    @Synchronized
    fun countMembers(count: Int) {
        memberCount = count
    }

    @Synchronized
    fun countRequests(count: Int) {
        requestCount = count
    }

    @Synchronized
    fun memberJoined() {
        memberCount++
    }

    @Synchronized
    fun memberLeft() {
        memberCount = (memberCount - 1).coerceAtLeast(0)
    }

    @Synchronized
    fun requestMade() {
        requestCount++
    }

    @Synchronized
    fun requestSettled() {
        requestCount = (requestCount - 1).coerceAtLeast(0)
    }

    private inline fun edit(change: () -> Unit) {
        change()
        hasUnsavedChanges = true
    }
}
