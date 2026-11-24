package zel.room.unit.pet

import zel.game.items.Item
import zel.game.items.MonsterPlantSeeds
import zel.game.pets.MonsterPlant
import zel.game.pets.Pet
import zel.game.pets.PetBehaviour
import zel.game.pets.PetBoxes
import zel.game.pets.PetPackage
import zel.game.pets.PetsModule
import zel.room.RoomScope
import zel.room.furni.behaviour.special.SpecialPart
import zel.room.furni.behaviour.special.SpecialPets
import zel.room.map.Point

class GameSpecialPets(
    private val pets: RoomPets,
    private val manager: () -> PetsModule?,
) : SpecialPets {
    override val plants: Boolean get() = manager() != null

    override fun canHatch(pack: PetPackage): Boolean = manager()?.catalog?.kind(pack.petType) != null

    override fun nestOf(pack: PetPackage): Item? =
        pack.nestItemName?.let {
            manager()
                ?.services
                ?.items()
                ?.getItem(it)
        }

    override fun canHatchBox(boxName: String): Boolean =
        manager()?.let { PetBoxes.canHatch(boxName, it.catalog) } == true

    override fun newFromBox(
        playerId: Int,
        boxName: String,
        name: String,
    ): Pet? {
        val player = pets.host.player(playerId) ?: return null
        if (player.client == null) return null
        val module = manager() ?: return null
        val maker = module.maker
        val ownerId = player.profile.id
        val box = PetBoxes.of(boxName) ?: return null
        return when {
            module.catalog.behaviourOf(box.type) == PetBehaviour.GNOME -> maker.gnome(name, ownerId)
            module.catalog.behaviourOf(box.type) == PetBehaviour.LEPRECHAUN -> maker.leprechaun(name, ownerId)
            box.palette != null -> maker.ofBreed(box.type, box.palette, name, ownerId)
            else -> maker.ofRandomPalette(box.type, name, ownerId)
        }
    }

    override fun newPlant(
        playerId: Int,
        seed: Item,
        rarity: String,
    ): Pet? {
        val player = pets.host.player(playerId) ?: return null
        val carried = rarity.trim().toIntOrNull()?.takeIf { it in 0..MonsterPlant.TOP_RARITY }
        val level = carried ?: MonsterPlantSeeds.rolledLevel(seed).toInt()
        return manager()?.maker?.plant(player.profile.id, level)
    }

    override fun newPackaged(
        playerId: Int,
        pack: PetPackage,
        name: String,
    ): Pet? {
        val player = pets.host.player(playerId) ?: return null
        if (player.client == null) return null
        return manager()?.maker?.ofBreed(pack.petType, pack.palette, name, player.profile.id, pack.color)
    }

    override fun plant(
        scope: RoomScope,
        plant: Pet,
        tile: Point,
    ) = pets.planted(scope, plant, tile)

    override fun hatch(
        scope: RoomScope,
        pet: Pet,
        tile: Point,
        direction: Int,
    ) = pets.hatched(scope, pet, tile, direction)

    override fun toInventory(
        pet: Pet,
        acquired: Boolean,
    ) = pets.toInventory(pet, acquired)

    override fun supplement(
        scope: RoomScope,
        playerId: Int,
        petId: Int,
        type: Int,
    ) = pets.plantCare.supplement(scope, playerId, petId, type)

    override fun treatmentWith(
        scope: RoomScope,
        playerId: Int,
        petId: Int,
        item: Item,
    ): (() -> Unit)? =
        pets.plantCare.treatmentWith(scope, playerId, petId, item)
            ?: horseTreatment(scope, playerId, petId, item)

    private fun horseTreatment(
        scope: RoomScope,
        playerId: Int,
        petId: Int,
        item: Item,
    ): (() -> Unit)? = pets.horses.use(scope, playerId, petId, item)

    override fun claimForCompost(
        scope: RoomScope,
        playerId: Int,
        petId: Int,
    ): SpecialPart.Spot? {
        val placed = pets.byId(scope, petId) ?: return null
        val plant = placed.pet as? MonsterPlant ?: return null
        val unit = pets.unitOf(scope, placed) ?: return null
        if (plant.userId != playerId || !plant.isDead || placed.plant.composting) return null
        placed.plant.composting = true
        return SpecialPart.Spot(unit.tile, unit.z, unit.bodyDirection)
    }

    override fun compostDefinition(): Item? =
        manager()
            ?.services
            ?.items()
            ?.byBehaviour(PlantCare.COMPOST)

    override fun deleteComposted(petId: Int) = pets.deleteRow(petId)

    override fun composted(
        scope: RoomScope,
        petId: Int,
    ) {
        pets.byId(scope, petId)?.let { pets.remove(scope, it) }
    }

    override fun compostFailed(
        scope: RoomScope,
        petId: Int,
    ) {
        pets.byId(scope, petId)?.plant?.composting = false
    }
}
