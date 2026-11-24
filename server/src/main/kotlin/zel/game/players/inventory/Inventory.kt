package zel.game.players.inventory

import zel.game.catalog.marketplace.Marketplace
import zel.game.players.Player
import zel.server.GameServices
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(Inventory::class.java)

class Inventory(
    services: GameServices,
    unseenStore: UnseenItems.Store,
    player: Player,
    marketplace: Marketplace,
) {
    private class Part<T : Any>(
        private val name: String,
        load: () -> T,
        private val save: (T) -> Unit,
    ) {
        private var value: T? =
            @Suppress("TooGenericExceptionCaught") // one failing part must not stop the others from loading
            try {
                load()
            } catch (e: Exception) {
                log.error("The {} of an inventory could not be loaded", name, e)
                null
            }

        val loaded: T get() = checkNotNull(value) { "$name not loaded" }

        fun dispose() {
            val held = value ?: return
            value = null
            @Suppress("TooGenericExceptionCaught") // one failing part must not stop the others from saving
            try {
                save(held)
            } catch (e: Exception) {
                log.error("The {} of an inventory could not be saved", name, e)
            }
        }
    }

    val unseen = UnseenItems(player.profile.id, unseenStore)

    init {
        @Suppress("TooGenericExceptionCaught") // the rest of the inventory still loads
        try {
            unseen.load()
        } catch (e: Exception) {
            log.error("The unseen items of an inventory could not be loaded", e)
        }
    }

    private val badgePart = Part("badges", { BadgeCase(services, player) }, BadgeCase::dispose)
    private val botPart = Part("bots", { BotInventory(services, player) }, BotInventory::dispose)
    private val effectPart = Part("effects", { AvatarEffects(services, player) }, AvatarEffects::dispose)
    private val furniPart = Part("furni", { FurniStash(services, player) }, FurniStash::dispose)
    private val petPart = Part("pets", { PetInventory(services, player) }, PetInventory::dispose)
    private val wardrobePart = Part("wardrobe", { Wardrobe(services, player.profile.id) }, Wardrobe::dispose)

    val badges: BadgeCase get() = badgePart.loaded
    val bots: BotInventory get() = botPart.loaded
    val effects: AvatarEffects get() = effectPart.loaded
    val furni: FurniStash get() = furniPart.loaded
    val pets: PetInventory get() = petPart.loaded
    val wardrobe: Wardrobe get() = wardrobePart.loaded

    val listings = OwnListings(marketplace, marketplace.ownListings(player))

    fun dispose() {
        listOf(badgePart, botPart, effectPart, furniPart, petPart, wardrobePart).forEach { it.dispose() }
    }

    companion object {
        @Volatile
        var maximumItems = 10000
    }
}
