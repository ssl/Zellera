package zel.protocol.incoming.guilds

import zel.util.MILLIS_PER_SECOND
import java.util.concurrent.ConcurrentHashMap

internal class GuildForumPostRate {
    private val lastPostSecond = ConcurrentHashMap<Int, Long>()

    fun mayPost(
        userId: Int,
        nowMillis: Long,
    ): Boolean {
        val last = lastPostSecond[userId] ?: return true
        return nowMillis / MILLIS_PER_SECOND - last >= WAIT_SECONDS
    }

    fun posted(
        userId: Int,
        nowMillis: Long,
    ) {
        lastPostSecond[userId] = nowMillis / MILLIS_PER_SECOND
    }

    private companion object {
        const val WAIT_SECONDS = 30L
    }
}
