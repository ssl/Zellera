package zel.room.rights

import zel.game.players.Player
import zel.protocol.generated.IgnoreResultOut
import zel.protocol.generated.IgnoreResultOutCodec

class Ignores(
    private val inRoom: (Int) -> Boolean,
) {
    fun ignore(
        player: Player,
        targetId: Int,
        reply: Reply,
    ) {
        if (targetId <= 0 || targetId == player.profile.id) return
        if (!player.stats.contacts.ignore(targetId)) return
        reply.send(IgnoreResultOutCodec, IgnoreResultOut(IGNORED, targetId))
    }

    fun unignore(
        player: Player,
        targetId: Int,
        reply: Reply,
    ) {
        val stats = player.stats
        if (!inRoom(player.profile.id) || !stats.contacts.userIgnored(targetId)) return
        stats.contacts.unignore(targetId)
        reply.send(IgnoreResultOutCodec, IgnoreResultOut(UNIGNORED, targetId))
    }

    companion object {
        const val IGNORED = 1
        const val UNIGNORED = 3
    }
}
