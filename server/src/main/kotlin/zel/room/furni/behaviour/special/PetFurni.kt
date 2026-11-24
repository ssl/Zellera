package zel.room.furni.behaviour.special

import zel.game.items.Item
import zel.game.pets.Pet
import zel.game.pets.PetBoxes
import zel.game.pets.PetMessages
import zel.game.pets.PetNameLimits
import zel.game.pets.PetPackage
import zel.game.pets.PetPackageNaming
import zel.protocol.generated.FurniListRemoveOut
import zel.protocol.generated.FurniListRemoveOutCodec
import zel.protocol.generated.OpenPetPackageRequestedOutCodec
import zel.protocol.generated.OpenPetPackageResultOut
import zel.protocol.generated.OpenPetPackageResultOutCodec
import zel.protocol.generated.PurchaseErrorOut
import zel.protocol.generated.PurchaseErrorOutCodec
import zel.protocol.generated.StuffData
import zel.protocol.outgoing.catalog.PurchaseError
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Toggleable
import zel.room.furni.Usable
import zel.room.furni.behaviour.state.Near
import zel.room.furni.place.StuffOnWire
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane
import kotlin.random.Random

class PetFurni(
    private val part: SpecialPart,
    private val pets: SpecialPets,
    private val packages: (String) -> PetPackage?,
) {
    val seed = Seed()
    val box = Package()
    val petBox = Box()

    abstract inner class Hatching :
        Usable,
        Toggleable,
        StuffOnWire {
        override fun toggle(
            scope: RoomScope,
            furni: RoomFurni,
            backward: Boolean,
        ): Boolean = false

        override fun toggleRandom(
            scope: RoomScope,
            furni: RoomFurni,
            random: Random,
        ): Boolean = false
    }

    inner class Seed : Hatching() {
        override fun onUse(
            scope: RoomScope,
            furni: RoomFurni,
            playerId: Int,
            param: Int,
        ) {
            if (furni.ownerId != playerId || !furni.isSpendable || !pets.plants) return
            val tile = furni.anchor ?: return
            val make = { pets.newPlant(playerId, furni.definition, furni.state) }
            val orphaned = { plant: Pet -> pets.toInventory(plant, acquired = false) }
            part.becomes(scope, furni, make, orphaned) { later, left, plant ->
                left.removed(later, SPENT_PICKER)
                plant?.let { pets.plant(later, it, tile) }
                left.heights(later)
            }
        }

        override fun stuff(furni: RoomFurni): StuffData =
            SpecialStuff.of(furni, SpecialStuff.MAP, StuffData.Value.MapData(listOf(RARITY to furni.state)))
    }

    inner class Package : Hatching() {
        override fun onUse(
            scope: RoomScope,
            furni: RoomFurni,
            playerId: Int,
            param: Int,
        ) {
            if (furni.ownerId != playerId) return
            val unit = scope.room.units.ofPlayer(playerId) ?: return
            val pet = packages(furni.definition.name) ?: return
            if (Near.adjacent(furni, unit.tile)) return askName(scope, playerId, furni.id, pet)
            part.of(scope.room).naming[playerId] = SpecialRoom.Naming(furni.id, scope.now())
            Near.walkBeside(scope, part.hooks, furni, unit)
        }

        override fun stuff(furni: RoomFurni): StuffData =
            SpecialStuff.legacy(furni, packages(furni.definition.name)?.figure().orEmpty())
    }

    inner class Box :
        Usable,
        Toggleable {
        private val plain: Toggleable? get() = part.behaviourOf(PLAIN_TYPE) as? Toggleable

        override fun onUse(
            scope: RoomScope,
            furni: RoomFurni,
            playerId: Int,
            param: Int,
        ) {
            val type = PetBoxes.of(boxName(furni))?.type ?: return
            scope.out.send(
                Lane.Now,
                Audience.Player(playerId),
                OpenPetPackageRequestedOutCodec,
                PetMessages.packageRequested(furni.id, type, 0, BOX_COLOUR),
            )
        }

        override fun toggle(
            scope: RoomScope,
            furni: RoomFurni,
            backward: Boolean,
        ): Boolean = plain?.toggle(scope, furni, backward) == true

        override fun toggleRandom(
            scope: RoomScope,
            furni: RoomFurni,
            random: Random,
        ): Boolean = plain?.toggleRandom(scope, furni, random) == true
    }

    fun tick(scope: RoomScope) {
        val naming = part.of(scope.room).naming
        naming.entries.toList().forEach { (playerId, wait) ->
            val unit = scope.room.units.ofPlayer(playerId)
            val furni = scope.room.furni[wait.furniId]
            val pet = furni?.definition?.name?.let(packages)
            if (unit == null || furni == null || pet == null) {
                naming.remove(playerId)
                return@forEach
            }
            if (unit.isWalking) return@forEach
            val beside = Near.adjacent(furni, unit.tile)
            if (beside) askName(scope, playerId, furni.id, pet)
            if (beside || scope.now() - wait.since >= WALK_START_MILLIS) naming.remove(playerId)
        }
    }

    private fun askName(
        scope: RoomScope,
        playerId: Int,
        furniId: Int,
        pet: PetPackage,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        OpenPetPackageRequestedOutCodec,
        PetMessages.packageRequested(furniId, pet.petType, pet.palette, pet.color),
    )

    fun open(
        scope: RoomScope,
        playerId: Int,
        furniId: Int,
        name: String,
    ) {
        val furni = scope.room.furni[furniId]
        val tile = furni?.anchor
        val hatching = furni?.behaviour
        if (furni == null || tile == null || furni.ownerId != playerId || !furni.isSpendable) {
            return answer(scope, playerId, furniId)
        }
        if (hatching !== box && hatching !== petBox) return answer(scope, playerId, furniId)
        val error = PetPackageNaming.validate(name, PetNameLimits.minimum, PetNameLimits.maximum)
        if (error != PetPackageNaming.CLOSE_WIDGET) {
            val info = PetPackageNaming.info(error, name, PetNameLimits.minimum, PetNameLimits.maximum)
            return answer(scope, playerId, furniId, error, info)
        }
        if (hatching === box) openPackage(scope, furni, tile, name) else openBox(scope, furni, tile, name)
    }

    private fun openPackage(
        scope: RoomScope,
        furni: RoomFurni,
        tile: Point,
        name: String,
    ) {
        val playerId = furni.ownerId
        val pack = packages(furni.definition.name) ?: return answer(scope, playerId, furni.id)
        if (!pets.canHatch(pack)) return refuse(scope, playerId, furni.id)
        val spot = SpecialPart.Spot(tile, furni.z, PetBoxes.NEST_ROTATION)
        val roomId = scope.room.info.id
        val make = { hatchling(roomId, playerId, pack, name, spot) }
        val orphaned = { hatched: Hatchling -> pets.toInventory(hatched.pet, acquired = true) }
        part.becomes(scope, furni, make, orphaned) { later, left, hatched ->
            hatched?.nest?.let { part.arrive(later, playerId, hatched.ownerName, it, spot, NEST_STATE) }
            left.removed(later, playerId)
            left.heights(later)
            hatched?.let { pets.hatch(later, it.pet, tile, furni.direction) }
            answer(later, playerId, furni.id)
        }
    }

    private fun openBox(
        scope: RoomScope,
        furni: RoomFurni,
        tile: Point,
        name: String,
    ) {
        val playerId = furni.ownerId
        val boxName = boxName(furni)
        if (!pets.canHatchBox(boxName)) return refuse(scope, playerId, furni.id)
        val make = { pets.newFromBox(playerId, boxName, name) }
        val orphaned = { pet: Pet -> pets.toInventory(pet, acquired = true) }
        part.becomes(scope, furni, make, orphaned) { later, left, pet ->
            pet?.let { pets.hatch(later, it, tile, 0) }
            left.removed(later, playerId)
            left.heights(later)
            answer(later, playerId, furni.id)
        }
    }

    private fun refuse(
        scope: RoomScope,
        playerId: Int,
        furniId: Int,
    ) {
        val error = PurchaseErrorOut(PurchaseError.GENERIC.code)
        scope.out.send(Lane.Now, Audience.Player(playerId), PurchaseErrorOutCodec, error)
        answer(scope, playerId, furniId)
    }

    fun useOnPet(
        scope: RoomScope,
        playerId: Int,
        furniId: Int,
        petId: Int,
    ) {
        val furni = scope.room.furni[furniId]?.takeIf { it.ownerId == playerId && it.isSpendable } ?: return
        val item = furni.definition
        if (pets.treatmentWith(scope, playerId, petId, item) == null) return
        // The treatment leaves the owner's list first, by the negated id the list shows it by.
        scope.out.send(Lane.Now, Audience.Player(playerId), FurniListRemoveOutCodec, FurniListRemoveOut(-furni.id))
        val roomId = scope.room.info.id
        part.spend(scope, furni, picker = playerId) {
            part.post(roomId) { later -> pets.treatmentWith(later, playerId, petId, item)?.invoke() }
        }
    }

    fun compost(
        scope: RoomScope,
        playerId: Int,
        petId: Int,
    ) {
        val spot = pets.claimForCompost(scope, playerId, petId) ?: return
        val roomId = scope.room.info.id
        part.persist(
            Runnable {
                if (!pets.deleteComposted(petId)) {
                    part.post(roomId) { later -> pets.compostFailed(later, petId) }
                    return@Runnable
                }
                val made = pets.compostDefinition()?.let { compostFurni(playerId, it, roomId, spot) }
                val ownerName = part.host.playerName(playerId)
                part.post(roomId) { later ->
                    made?.let { part.arrive(later, playerId, ownerName, it, spot, NEST_STATE) }
                    pets.composted(later, petId)
                }
            },
        )
    }

    private fun compostFurni(
        playerId: Int,
        definition: Item,
        roomId: Int,
        spot: SpecialPart.Spot,
    ): SpecialPart.NewFurni? {
        val id = part.host.createFurni(playerId, definition, NEST_STATE)
        if (id <= 0) return null
        part.host.putInRoom(id, roomId, spot.anchor.x, spot.anchor.y, spot.z, spot.direction)
        return SpecialPart.NewFurni(id, definition)
    }

    private fun boxName(furni: RoomFurni): String = furni.definition.name.lowercase()

    private fun hatchling(
        roomId: Int,
        playerId: Int,
        pack: PetPackage,
        name: String,
        spot: SpecialPart.Spot,
    ): Hatchling? {
        val pet = pets.newPackaged(playerId, pack, name) ?: return null
        val nest = pets.nestOf(pack)
        val id = nest?.let { part.host.createFurni(playerId, it, NEST_STATE) } ?: 0
        if (id > 0) part.host.putInRoom(id, roomId, spot.anchor.x, spot.anchor.y, spot.z, spot.direction)
        val placed = nest?.takeIf { id > 0 }?.let { SpecialPart.NewFurni(id, it) }
        return Hatchling(pet, placed, part.host.playerName(playerId))
    }

    private fun answer(
        scope: RoomScope,
        playerId: Int,
        furniId: Int,
        status: Int = PetPackageNaming.CLOSE_WIDGET,
        info: String = "",
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        OpenPetPackageResultOutCodec,
        OpenPetPackageResultOut(furniId, status, info),
    )

    private class Hatchling(
        val pet: Pet,
        val nest: SpecialPart.NewFurni?,
        val ownerName: String,
    )

    companion object {
        const val PET_BOX_TYPE = "pet.hatchbox"
        private const val PLAIN_TYPE = "default"
        private const val BOX_COLOUR = "ffffff"
        private const val NEST_STATE = ""

        private const val WALK_START_MILLIS = 1000L
        private const val RARITY = "rarity"
        private const val SPENT_PICKER = -1
    }
}
