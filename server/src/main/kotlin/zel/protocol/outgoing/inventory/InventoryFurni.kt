package zel.protocol.outgoing.inventory

import zel.game.items.GiftWrap
import zel.game.items.ItemInteraction
import zel.game.items.MusicDisc
import zel.game.items.OwnedFurni
import zel.game.items.ProductKind
import zel.game.pets.PetBoxes
import zel.protocol.generated.StuffData

object InventoryFurni {
    private const val CATEGORY_FLOOR = 3
    private const val CATEGORY_LANDSCAPE = 4
    private const val CATEGORY_WALLPAPER = 2
    private const val CATEGORY_POSTER = 6
    private const val CATEGORY_SONG_DISK = 8
    private const val CATEGORY_PRESENT = 9
    private const val CATEGORY_POST_IT = 5
    private const val CATEGORY_GNOME_BOX = 13
    private const val CATEGORY_GUILD_FURNI = 17
    private const val CATEGORY_MONSTER_PLANT_SEED = 19
    private const val CATEGORY_PLAIN = 1
    private const val CATEGORY_FURNI_CHEST = 24
    private const val CATEGORY_COIN_CHEST = 25
    private const val GIFT_COLOUR_FACTOR = 1000

    const val NEVER_EXPIRES = -1

    /** The room id of a furni in the inventory. */
    const val NOT_IN_A_ROOM = -1

    data class Entry(
        val stripId: Int,
        val type: String,
        val itemId: Int,
        val spriteId: Int,
        val category: Int,
        val stuffData: StuffData,
        val recyclable: Boolean,
        val tradable: Boolean,
        val groupable: Boolean,
        val sellable: Boolean,
        val floorExtra: Int?,
    )

    fun entry(roomItem: OwnedFurni): Entry {
        val base = roomItem.definition
        val (category, stuff) = categoryAndStuffData(roomItem)
        return Entry(
            stripId = roomItem.stripId,
            type = base.type.code,
            itemId = roomItem.id,
            spriteId = base.spriteId,
            category = category,
            stuffData = stuff,
            recyclable = base.canRecycle,
            tradable = base.canTrade,
            groupable = !roomItem.isLimited && base.stacksInInventory,
            sellable = sellable(roomItem),
            floorExtra = if (base.type == ProductKind.FLOOR_FURNI) floorExtra(roomItem) else null,
        )
    }

    fun sellable(roomItem: OwnedFurni): Boolean = roomItem.definition.canTrade && roomItem.definition.canSell

    fun floorExtra(roomItem: OwnedFurni): Int {
        if (roomItem.definition.interaction.name == ItemInteraction.GIFT) return giftWrapping(roomItem.state)
        if (roomItem.definition.name == SONG_DISK) return MusicDisc.songId(roomItem.state)
        if (roomItem.definition.interaction.isTeleport) {
            return roomItem.definition.services
                .items()
                .teleportPartner(roomItem.id)
        }
        return 0
    }

    private fun giftWrapping(state: String): Int {
        val wrap = GiftWrap.read(state)
        return wrap.paper * GIFT_COLOUR_FACTOR + wrap.ribbon
    }

    fun category(roomItem: OwnedFurni): Int {
        val interaction = roomItem.definition.interaction
        return when {
            PetBoxes.isGnome(roomItem.definition.name) -> CATEGORY_GNOME_BOX
            interaction.name == ItemInteraction.GIFT -> CATEGORY_PRESENT
            interaction.name == ItemInteraction.POST_IT -> CATEGORY_POST_IT
            interaction.name == ItemInteraction.GUILD_FURNI -> CATEGORY_GUILD_FURNI
            interaction.name == ItemInteraction.MONSTER_PLANT_SEED -> CATEGORY_MONSTER_PLANT_SEED
            interaction.isFurniStorage -> CATEGORY_FURNI_CHEST
            interaction.isStorage -> CATEGORY_COIN_CHEST
            else -> CATEGORY_PLAIN
        }
    }

    fun hasStuffData(roomItem: OwnedFurni): Boolean {
        if (isSongDisk(roomItem)) return true
        val (_, stuff) = categoryAndStuffData(roomItem)
        return !(stuff.flags == 0 && stuff.value == StuffData.Value.Legacy(""))
    }

    private const val SONG_DISK = "song_disk"

    private fun isSongDisk(roomItem: OwnedFurni) = roomItem.definition.name == SONG_DISK

    fun categoryAndStuffData(roomItem: OwnedFurni): Pair<Int, StuffData> {
        val specialCategory =
            when (roomItem.definition.name) {
                "landscape" -> CATEGORY_LANDSCAPE
                "floor" -> CATEGORY_FLOOR
                "wallpaper" -> CATEGORY_WALLPAPER
                "poster" -> CATEGORY_POSTER
                SONG_DISK -> CATEGORY_SONG_DISK
                else -> null
            }
        if (specialCategory != null) {
            val state = if (isSongDisk(roomItem)) "" else roomItem.state
            return specialCategory to StuffData(0, StuffData.Value.Legacy(state), null)
        }
        return category(roomItem) to inventoryStuff(roomItem)
    }

    private fun inventoryStuff(roomItem: OwnedFurni): StuffData {
        val stuff =
            roomItem.definition.services.inventoryStuff
                .of(roomItem)
        val value = stuff.value
        if (stuff.flags and FORMAT_MASK != 0 || value !is StuffData.Value.Legacy) return stuff
        val trophy = roomItem.definition.interaction.name == ItemInteraction.TROPHY
        val floor = roomItem.definition.type == ProductKind.FLOOR_FURNI && !trophy
        return if (floor || value.state == "0") stuff.copy(value = StuffData.Value.Legacy("")) else stuff
    }

    private const val FORMAT_MASK = 0xFF
}
