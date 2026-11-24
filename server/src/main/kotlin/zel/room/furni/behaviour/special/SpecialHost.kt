package zel.room.furni.behaviour.special

import zel.game.items.CrackableKind
import zel.game.items.Item
import zel.game.pets.PetPackage

/** Members marked "persistence" run on the persistence executor, never under a room lock. */
@Suppress("TooManyFunctions") // one per server service
interface SpecialHost {
    fun mayEditAds(playerId: Int): Boolean = true

    fun checksDimmerColour(): Boolean = true

    fun deleteSpent(
        roomId: Int,
        rowId: Int,
    ): Boolean = true

    fun mysteryPrize(
        playerId: Int,
        colour: String,
    ) {}

    fun crackable(definitionId: Int): CrackableKind? = null

    fun playerName(playerId: Int): String = ""

    fun crackPrize(definitionId: Int): Item? = null

    fun createFurni(
        ownerId: Int,
        definition: Item,
        state: String,
    ): Int = 0

    @Suppress("LongParameterList") // one per furni column
    fun putInRoom(
        furniId: Int,
        roomId: Int,
        x: Int,
        y: Int,
        z: Double,
        direction: Int,
    ) {}

    fun giveFurni(
        playerId: Int,
        definition: Item,
    ) {}

    /** Runs on the persistence executor. */
    fun membership(
        playerId: Int,
        type: String,
        seconds: Int,
    ) {}

    fun wearEffect(
        playerId: Int,
        effect: Int,
    ) {}

    fun petPackage(itemName: String): PetPackage? = null

    companion object {
        val None: SpecialHost = object : SpecialHost {}
    }
}
