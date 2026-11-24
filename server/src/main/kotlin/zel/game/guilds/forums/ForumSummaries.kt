package zel.game.guilds.forums

import zel.game.guilds.Guild
import zel.game.players.Player
import zel.game.players.PlayerProfile

class ForumSummaries(
    private val threads: ForumThreadStore,
    private val marks: ForumReadMarks,
    private val profile: (Int) -> PlayerProfile?,
    private val clock: () -> Int,
) {
    fun of(
        guild: Guild,
        viewer: Player,
    ): ForumSummary {
        val snapshot = threads.threadList(guild.id)
        val last = snapshot.mapNotNull { it.lastPost }.maxByOrNull { it.createdAt }
        return ForumSummary(
            guild.id,
            guild.name,
            guild.description,
            guild.badge,
            snapshot.size,
            snapshot.sumOf { it.postCount },
            marks.unreadPosts(viewer.profile.id, guild.id),
            last?.id ?: NONE,
            last?.authorId ?: NONE,
            last?.let { profile(it.authorId)?.name }.orEmpty(),
            if (last != null) clock() - last.createdAt else 0,
        )
    }

    private companion object {
        const val NONE = -1
    }
}
