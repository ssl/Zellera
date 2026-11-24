package zel.game.guilds

data class ForumRules(
    val read: ForumLevel = ForumLevel.ADMINS,
    val postMessages: ForumLevel = ForumLevel.ADMINS,
    val startThreads: ForumLevel = ForumLevel.ADMINS,
    val moderate: ForumLevel = ForumLevel.ADMINS,
) {
    companion object {
        /** The owner always reads, whatever the level. */
        val READ_LEVELS = setOf(ForumLevel.EVERYONE, ForumLevel.MEMBERS, ForumLevel.ADMINS)

        val MODERATE_LEVELS = setOf(ForumLevel.ADMINS, ForumLevel.OWNER)

        /** The rules from four level codes, or null when one is not offered for its setting. */
        fun fromCodes(
            read: Int,
            postMessages: Int,
            startThreads: Int,
            moderate: Int,
        ): ForumRules? {
            val readLevel = ForumLevel.fromCode(read)?.takeIf { it in READ_LEVELS } ?: return null
            val post = ForumLevel.fromCode(postMessages) ?: return null
            val start = ForumLevel.fromCode(startThreads) ?: return null
            val moderateLevel = ForumLevel.fromCode(moderate)?.takeIf { it in MODERATE_LEVELS } ?: return null
            return ForumRules(readLevel, post, start, moderateLevel)
        }
    }
}
