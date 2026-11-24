package zel.game.players.inventory

import zel.game.items.Item
import zel.game.items.OwnedFurni
import zel.game.players.Player
import zel.protocol.generated.FurniListInvalidateOut
import zel.protocol.generated.FurniListInvalidateOutCodec
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.server.GameServices
import kotlin.math.abs

class FurniStash(
    private val services: GameServices,
    private val player: Player,
) {
    private val store = InventoryFurniStore(services)
    private val byId = HashMap<Int, OwnedFurni>(store.read(player.profile.id))

    val count: Int get() = synchronized(byId) { byId.size }

    val all: List<OwnedFurni> get() = synchronized(byId) { byId.values.toList() }

    operator fun get(itemId: Int): OwnedFurni? = synchronized(byId) { byId[abs(itemId)] }

    fun put(item: OwnedFurni?) {
        if (item == null) return
        synchronized(byId) { byId[item.id] = item }
        changed()
    }

    fun putAll(items: Collection<OwnedFurni?>) {
        synchronized(byId) { items.filterNotNull().forEach { byId[it.id] = it } }
        changed()
    }

    fun deliver(item: OwnedFurni) {
        put(item)
        player.client?.send(AddRoomItem(item))
        player.client?.send(FurniListInvalidateOutCodec, FurniListInvalidateOut)
    }

    fun remove(itemId: Int) {
        synchronized(byId) { byId.remove(itemId) }
        changed()
    }

    fun remove(item: OwnedFurni) = remove(item.id)

    fun takeOut(itemId: Int): OwnedFurni? = synchronized(byId) { byId.remove(itemId) }

    fun takeFirstOf(base: Item): OwnedFurni? =
        synchronized(byId) {
            byId.values.firstOrNull { it.definition === base }?.also { byId.remove(it.id) }
        }.also { changed() }

    fun takeAll(requested: Collection<OwnedFurni?>?): Boolean {
        if (requested.isNullOrEmpty()) return false
        val unique = requested.toSet()
        if (unique.size != requested.size) return false
        synchronized(byId) {
            if (unique.any { it == null || byId[it.id] !== it }) return false
            unique.filterNotNull().forEach { byId.remove(it.id) }
        }
        changed()
        return true
    }

    fun putBack(requested: Collection<OwnedFurni?>?) {
        if (requested.isNullOrEmpty()) return
        synchronized(byId) { requested.filterNotNull().forEach { byId[it.id] = it } }
    }

    fun dispose() {
        synchronized(byId) {
            if (byId.isNotEmpty()) store.writeChanged(byId.values)
            byId.clear()
        }
    }

    private fun changed() = services.game.accountLevels.inputChanged(player.profile.id)
}
