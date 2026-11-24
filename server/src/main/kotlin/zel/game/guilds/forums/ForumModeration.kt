package zel.game.guilds.forums

import zel.util.MILLIS_PER_SECOND

data class ForumModeration(
    val adminId: Int = 0,
    val at: Int = 0,
) {
    fun wireValue(now: Int): Int = if (adminId > 0 && at > 0) now - (at / MILLIS_PER_SECOND).toInt() else now
}
