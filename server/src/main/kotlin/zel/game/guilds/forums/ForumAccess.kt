package zel.game.guilds.forums

import zel.game.guilds.ForumLevel
import zel.game.guilds.Guild
import zel.game.guilds.MemberLevel
import zel.game.guilds.Membership

class ForumAccess(
    private val guild: Guild,
    userId: Int,
    private val member: Membership?,
    val isStaff: Boolean,
) {
    val isOwner: Boolean = guild.ownerId == userId || isStaff

    private val isAdminOrOwner: Boolean get() = isOwner || member?.role?.isAdmin == true

    val mayRead: Boolean get() = isOwner || passes(guild.forumRules.read)
    val mayPostMessage: Boolean get() = passes(guild.forumRules.postMessages)
    val mayStartThread: Boolean get() = passes(guild.forumRules.startThreads)

    /** Owners, staff and officers see hidden threads and posts and may hide or show them. */
    val maySeeHidden: Boolean get() = isOwner || member?.role == MemberLevel.OFFICER

    val isKnownToGroup: Boolean get() = isOwner || member != null

    /**
     * Moving content from [from] to [to]: staff anything; a group's own moderators (only the owner if the forum
     * says so) between open and hidden by the group. What staff hid, only staff bring back.
     */
    fun mayModerate(
        from: ForumState,
        to: ForumState,
    ): Boolean =
        isStaff ||
            (
                from != ForumState.HIDDEN_BY_STAFF && to != ForumState.HIDDEN_BY_STAFF && maySeeHidden &&
                    (guild.forumRules.moderate != ForumLevel.OWNER || isOwner)
            )

    val mayPinOrLock: Boolean
        get() =
            isStaff ||
                when (guild.forumRules.moderate) {
                    ForumLevel.OWNER -> isOwner
                    ForumLevel.ADMINS -> isAdminOrOwner
                    ForumLevel.EVERYONE, ForumLevel.MEMBERS -> true
                }

    private fun passes(level: ForumLevel): Boolean =
        isStaff ||
            when (level) {
                ForumLevel.EVERYONE -> true
                ForumLevel.MEMBERS -> member?.role?.isMember == true
                ForumLevel.ADMINS -> member?.role?.isAdmin == true
                ForumLevel.OWNER -> isOwner
            }

    val readRefusal: String
        get() =
            if (mayRead) {
                ""
            } else {
                when (guild.forumRules.read) {
                    ForumLevel.MEMBERS -> NOT_MEMBER
                    ForumLevel.ADMINS -> NOT_ADMIN
                    ForumLevel.EVERYONE, ForumLevel.OWNER -> NOT_OWNER
                }
            }

    fun permissions(): ForumPermissions {
        val rules = guild.forumRules
        val admin = member != null && (member.role.isAdmin || isOwner)
        return ForumPermissions(
            rules.read.code,
            rules.postMessages.code,
            rules.startThreads.code,
            rules.moderate.code,
            readRefusal,
            actionRefusal(rules.postMessages, admin),
            actionRefusal(rules.startThreads, admin),
            when {
                rules.moderate == ForumLevel.OWNER && !isOwner -> NOT_OWNER
                !admin && !isStaff -> NOT_ADMIN
                else -> ""
            },
            isOwner,
            if (rules.moderate == ForumLevel.OWNER) isOwner else isOwner || admin,
        )
    }

    private fun actionRefusal(
        level: ForumLevel,
        admin: Boolean,
    ): String =
        when {
            isStaff -> ""
            level == ForumLevel.MEMBERS && member?.role?.isMember != true -> NOT_MEMBER
            level == ForumLevel.ADMINS && !admin -> NOT_ADMIN
            level == ForumLevel.OWNER && !isOwner -> NOT_OWNER
            else -> ""
        }

    private companion object {
        const val NOT_MEMBER = "not_member"
        const val NOT_ADMIN = "not_admin"
        const val NOT_OWNER = "not_owner"
    }
}
