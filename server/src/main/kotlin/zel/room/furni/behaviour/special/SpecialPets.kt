package zel.room.furni.behaviour.special

import zel.game.items.Item
import zel.game.pets.Pet
import zel.game.pets.PetPackage
import zel.room.RoomScope
import zel.room.map.Point

/** The `new` members run on the persistence executor. */
@Suppress("TooManyFunctions") // one member per pet need
interface SpecialPets {
    val plants: Boolean get() = false

    fun canHatch(pack: PetPackage): Boolean = false

    fun nestOf(pack: PetPackage): Item? = null

    fun canHatchBox(boxName: String): Boolean = false

    fun newFromBox(
        playerId: Int,
        boxName: String,
        name: String,
    ): Pet? = null

    fun newPlant(
        playerId: Int,
        seed: Item,
        rarity: String,
    ): Pet? = null

    fun newPackaged(
        playerId: Int,
        pack: PetPackage,
        name: String,
    ): Pet? = null

    fun plant(
        scope: RoomScope,
        plant: Pet,
        tile: Point,
    ) {}

    fun hatch(
        scope: RoomScope,
        pet: Pet,
        tile: Point,
        direction: Int,
    ) {}

    /** Runs off the room lock. */
    fun toInventory(
        pet: Pet,
        acquired: Boolean,
    ) {}

    fun supplement(
        scope: RoomScope,
        playerId: Int,
        petId: Int,
        type: Int,
    ) {}

    fun treatmentWith(
        scope: RoomScope,
        playerId: Int,
        petId: Int,
        item: Item,
    ): (() -> Unit)? = null

    fun claimForCompost(
        scope: RoomScope,
        playerId: Int,
        petId: Int,
    ): SpecialPart.Spot? = null

    fun compostDefinition(): Item? = null

    /** Deletes a composted plant's row, on the persistence executor; false when it did not delete. */
    fun deleteComposted(petId: Int): Boolean = false

    fun composted(
        scope: RoomScope,
        petId: Int,
    ) {}

    fun compostFailed(
        scope: RoomScope,
        petId: Int,
    ) {}

    companion object {
        val None: SpecialPets = object : SpecialPets {}
    }
}
