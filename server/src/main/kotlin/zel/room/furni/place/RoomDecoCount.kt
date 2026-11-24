package zel.room.furni.place

import zel.game.achievements.AchievementProgress
import zel.game.achievements.GameEvents
import zel.game.achievements.countFor
import zel.game.items.CreditFurni
import zel.game.players.Player
import zel.room.RoomScope

/** Commits the achievement steps off the lock, in order per player. */
class RoomDecoCount(
    private val players: (Int) -> Player?,
    private val achievements: () -> AchievementProgress,
    private val later: (playerId: Int, job: Runnable) -> Unit,
) : PlacedCount {
    override fun placed(
        scope: RoomScope,
        ownerId: Int,
    ) {
        val player = players(ownerId) ?: return
        val furni = scope.room.furni.all()
        raise(player, GameEvents.ROOM_FURNI_COUNT, furni.size)
        raise(player, GameEvents.ROOM_FURNI_KINDS, furni.distinctBy { it.definition.id }.size)
        val credits = furni.filter { it.ownerId == ownerId }.sumOf { CreditFurni.credits(it.definition) }
        raise(player, GameEvents.ROOM_CREDIT_VALUE, credits)
    }

    override fun anyPlaced(
        scope: RoomScope,
        placerId: Int,
    ) {
        val furni = scope.room.furni.all()
        val holes = furni.count { it.definition.name.startsWith(HOLE) }
        players(placerId)?.let { raise(it, GameEvents.ROOM_HOLE_FURNI, holes) }
        val owner = players(scope.room.info.ownerId) ?: return
        val patches = furni.count { it.definition.name == SNOWBOARD_PATCH }
        later(owner.profile.id) {
            val progress = achievements()
            for (slopes in progress.forEvent(GameEvents.ROOM_SLOPES)) {
                if (patches > maxOf(0, owner.stats.progressOf(slopes))) progress.progress(owner, slopes, 1)
            }
        }
    }

    override fun noteLeft(
        scope: RoomScope,
        writerId: Int,
    ) {
        val owner = scope.room.info.ownerId
        if (owner == writerId) return
        later(owner) { achievements().countFor(owner, GameEvents.ROOM_NOTE_RECEIVED) }
        later(writerId) { achievements().countFor(writerId, GameEvents.ROOM_NOTE_LEFT) }
    }

    private fun raise(
        player: Player,
        event: String,
        total: Int,
    ) = later(player.profile.id) {
        val progress = achievements()
        for (achievement in progress.forEvent(event)) {
            val shown = maxOf(0, player.stats.progressOf(achievement))
            if (total > shown) progress.progress(player, achievement, total - shown)
        }
    }

    private companion object {
        const val HOLE = "hole"
        const val SNOWBOARD_PATCH = "snowb_slope"
    }
}
