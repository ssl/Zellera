package zel.wired.effect.user

import zel.game.items.Item
import zel.game.items.ItemServices
import zel.game.permissions.Privilege
import zel.net.clients.ClientConnections
import zel.room.RoomScope
import zel.room.entry.RoomLeaving
import zel.room.furni.FurniBehaviour
import zel.room.furni.transit.TransitData
import zel.room.life.RoomLife
import zel.room.rights.RightsStore
import zel.room.rights.RoomMutes
import zel.room.unit.RoomUnit
import zel.util.MILLIS_PER_SECOND
import zel.util.SECONDS_PER_MINUTE
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class GameUserEffects(
    private val leaving: RoomLeaving,
    private val life: RoomLife,
    private val mutes: RoomMutes,
    private val rights: RightsStore,
    private val links: TransitData,
    private val copies: CopyStore,
    private val services: Services,
) : UserEffectHost {
    class Services(
        val clients: ClientConnections,
        val items: () -> ItemServices?,
        val persist: (Runnable) -> Unit,
        val behaviourOf: (String) -> FurniBehaviour,
        /** Into the room past its door, on the game's thread pool, then the callback; false when the player is gone. */
        val travel: (RoomScope, Int, Int, Int, () -> Unit) -> Boolean,
        val teleportLandMillis: () -> Long = { Teleports.LAND_MILLIS },
        val rewards: RewardLog? = null,
    )

    /** Kept apart from the box so a rebuilt box keeps its count; dropped when the box's room unloads. */
    private val rewardCounts = ConcurrentHashMap<Int, AtomicInteger>()

    override fun kickOut(
        scope: RoomScope,
        unit: RoomUnit,
    ) = leaving.leave(scope, unit)

    override fun kickException(
        scope: RoomScope,
        playerId: Int,
    ): String? {
        val player = services.clients.onlinePlayer(playerId)
        val key =
            when {
                playerId == scope.room.info.ownerId -> OWNER_EXCEPTION
                player?.hasPermission(Privilege.ROOM_KICK_IMMUNE) == true -> STAFF_EXCEPTION
                else -> return null
            }
        return player
            ?.services
            ?.texts
            ?.text(key)
            .orEmpty()
    }

    override fun mute(
        scope: RoomScope,
        playerId: Int,
        minutes: Int,
    ) {
        val ends = (System.currentTimeMillis() / MILLIS_PER_SECOND).toInt() + minutes * SECONDS_PER_MINUTE
        mutes.of(scope.room).players[playerId] = ends
        val roomId = scope.room.info.id
        services.persist(Runnable { rights.mute(roomId, playerId, ends) })
    }

    override fun wake(
        scope: RoomScope,
        unit: RoomUnit,
    ) = life.active(scope, unit)

    override fun linkerPair(furniId: Int): LinkerPair? =
        links.partnerOf(furniId)?.takeIf { it.roomId > 0 }?.let { LinkerPair(it.roomId, it.furniId) }

    override fun bannedFrom(
        roomId: Int,
        playerId: Int,
    ): Boolean {
        val now = (System.currentTimeMillis() / MILLIS_PER_SECOND).toInt()
        return rights.bans(roomId, now).any { it.id == playerId }
    }

    override fun teleportLandMillis(): Long = services.teleportLandMillis()

    override fun travel(
        scope: RoomScope,
        playerId: Int,
        roomId: Int,
        furniId: Int,
        afterwards: () -> Unit,
    ): Boolean = services.travel(scope, playerId, roomId, furniId, afterwards)

    override fun placeCopy(
        ownerId: Int,
        definition: Item,
        state: String,
        place: CopyPlace,
    ): Int {
        val items = services.items()?.items() ?: return 0
        return copies.create(place) { c -> items.newFurni(c, ownerId, definition, 0, 0, state)?.id } ?: 0
    }

    override fun behaviour(type: String): FurniBehaviour = services.behaviourOf(type)

    override fun copiesOf(roomId: Int): Set<Int> = copies.ofRoom(roomId)

    override fun destroyCopies(rowIds: List<Int>) = services.persist(Runnable { copies.destroy(rowIds) })

    override fun giveReward(
        playerId: Int,
        reward: StaffReward,
    ): Boolean {
        val player = services.clients.onlinePlayer(playerId) ?: return false
        if (!reward.isBadge) return false
        return player.inventory.badges.award(reward.code) != null
    }

    override fun ownsReward(
        playerId: Int,
        reward: StaffReward,
    ): Boolean =
        reward.isBadge &&
            services.clients
                .onlinePlayer(playerId)
                ?.inventory
                ?.badges
                ?.owns(reward.code) == true

    override fun rewardsGiven(
        furniId: Int,
        playerId: Int,
    ): List<GivenReward> = services.rewards?.given(furniId, playerId).orEmpty()

    override fun rewardCount(furniId: Int): Int = rewardCounter(furniId)?.get() ?: 0

    override fun loadRewardCounts(furniIds: Collection<Int>) {
        furniIds.forEach { rewardCounter(it) }
    }

    override fun forgetRewardCounts(furniIds: Collection<Int>) {
        rewardCounts.keys.removeAll(furniIds.toSet())
    }

    override fun noteReward(
        furniId: Int,
        playerId: Int,
        reward: GivenReward,
    ) {
        val log = services.rewards ?: return
        rewardCounter(furniId)?.incrementAndGet()
        services.persist(Runnable { log.note(furniId, playerId, reward) })
    }

    private fun rewardCounter(furniId: Int): AtomicInteger? {
        val log = services.rewards ?: return null
        return rewardCounts.computeIfAbsent(furniId) { AtomicInteger(log.count(it)) }
    }

    private companion object {
        const val OWNER_EXCEPTION = "wired.kick.spared.owner"
        const val STAFF_EXCEPTION = "wired.kick.spared.staff"
    }
}
