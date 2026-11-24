package zel.game.players.inventory

import zel.game.players.Player
import zel.game.players.PlayerBadge
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.protocol.outgoing.inventory.AddRoomItem.UnseenCategory
import zel.protocol.outgoing.inventory.InventoryBadges
import zel.protocol.outgoing.users.AddUserBadge
import zel.server.GameServices
import java.sql.Connection
import java.sql.SQLException

/** A staff badge of a rank the player doesn't hold is dropped when the case is opened. */
class BadgeCase(
    private val services: GameServices,
    private val player: Player,
) {
    private val store = BadgeStore(services)
    private val ownerId: Int get() = player.profile.id
    private val owned = LinkedHashSet<PlayerBadge>(kept())

    val all: List<PlayerBadge> get() = synchronized(owned) { owned.toList() }

    val worn: List<PlayerBadge> get() = all.filter { it.slot != 0 }.sortedBy { it.slot }

    fun owns(code: String): Boolean = find(code) != null

    fun find(code: String): PlayerBadge? =
        synchronized(owned) { owned.firstOrNull { it.code.equals(code, ignoreCase = true) } }

    fun add(badge: PlayerBadge) = alter { owned.add(badge) }

    fun remove(badge: PlayerBadge) = alter { owned.remove(badge) }

    fun award(
        code: String,
        asUnseen: Boolean = false,
    ): PlayerBadge? {
        if (owns(code)) return null
        val badge = PlayerBadge(0, code)
        if (!store.insert(ownerId, badge)) return null
        if (asUnseen) player.client?.send(AddRoomItem(badge.id, UnseenCategory.BADGE))
        add(badge)
        player.client?.send(AddUserBadge(badge, services.badgeCaches.ownerCounts))
        return badge
    }

    fun discard(badge: PlayerBadge) {
        remove(badge)
        store.delete(ownerId, badge.code)
        player.client?.send(InventoryBadges(player))
    }

    fun wearOnly(slots: Map<PlayerBadge, Int>) {
        val changed = (worn + slots.keys).distinct()
        changed.forEach { it.slot = slots[it] ?: 0 }
        val rows = changed.map { BadgeSlot(it.id, it.code, it.slot) }
        services.threading.run { store.updateSlots(ownerId, rows) }
    }

    fun dispose() = synchronized(owned) { owned.clear() }

    private fun alter(change: () -> Unit) {
        synchronized(owned) { change() }
        services.badgeCaches.invalidate()
        services.game.accountLevels.inputChanged(ownerId)
    }

    private fun kept(): List<PlayerBadge> {
        val ranks = checkNotNull(services.game.permissions).ranks
        val rankId = player.profile.rank.id
        val (stale, current) =
            store.owned(ownerId).partition {
                it.code in ranks.staffBadges && ranks.withBadge(it.code).none { r -> r.id == rankId }
            }
        stale.forEach { store.delete(ownerId, it.code) }
        return current
    }

    companion object {
        @Throws(SQLException::class)
        fun storeIn(
            services: GameServices,
            connection: Connection,
            ownerId: Int,
            code: String,
        ): PlayerBadge = PlayerBadge(0, code).also { BadgeStore(services).insert(connection, ownerId, it) }

        fun wornByOffline(
            services: GameServices,
            playerId: Int,
        ): List<PlayerBadge> = BadgeStore(services).wornOffline(playerId)
    }
}
