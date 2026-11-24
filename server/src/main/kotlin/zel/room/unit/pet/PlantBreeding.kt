package zel.room.unit.pet

import zel.game.achievements.GameEvents
import zel.game.items.MonsterPlantSeeds
import zel.game.pets.MonsterPlant
import zel.game.pets.PetAchievements
import zel.game.pets.PetEvent
import zel.game.pets.PetMessages
import zel.game.players.Player
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.protocol.generated.FurniListInvalidateOut
import zel.protocol.generated.FurniListInvalidateOutCodec
import zel.protocol.generated.PetBreedingOut
import zel.protocol.generated.PetBreedingOutCodec
import zel.protocol.generated.PetBreedingResultOutCodec
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane
import java.util.Random

class PlantBreeding(
    private val pets: RoomPets,
    private val store: PetStore,
    private val persist: (Runnable) -> Unit,
) {
    /** An open ask to breed with the plant it hangs on; it lasts [ASK_MILLIS] and a newer ask takes its place. */
    data class Proposal(
        val askerId: Int,
        val askerPlantId: Int,
        val askedAt: Long,
    )

    fun breedPets(
        scope: RoomScope,
        sender: Int,
        step: Int,
        petOne: Int,
        petTwo: Int,
    ) {
        if (petOne == petTwo) return
        val first = plant(scope, petOne) ?: return
        val second = plant(scope, petTwo) ?: return
        val own = listOf(first, second).firstOrNull { it.pet.userId == sender } ?: return
        val partner = if (own === first) second else first
        when (step) {
            STEP_ASK -> propose(scope, own, partner)
            STEP_CANCEL -> withdraw(scope, own, partner)
            STEP_CONFIRM -> agree(scope, sender, own, partner)
        }
    }

    private fun propose(
        scope: RoomScope,
        own: PlacedPlant,
        partner: PlacedPlant,
    ) {
        if (!own.pet.isBreedable || !partner.pet.isBreedable) return
        val asker = own.pet.userId
        val asked = partner.pet.userId
        if (asked == asker) return breed(scope, own, partner)
        val open = partner.pet.allowsBreeding && scope.room.units.ofPlayer(asked) != null
        if (!open) return answer(scope, asker, PetBreedingOut(TOLD_CANCELLED, 0, 0))
        partner.placed.proposal = Proposal(asker, own.pet.id, scope.now())
        answer(scope, asked, PetBreedingOut(TOLD_ASKED, partner.pet.id, own.pet.id))
    }

    /** The asker takes the ask back or the asked owner turns it down; anything else changes nothing. */
    private fun withdraw(
        scope: RoomScope,
        own: PlacedPlant,
        partner: PlacedPlant,
    ) {
        val holder =
            listOf(partner to own, own to partner)
                .firstOrNull { (asked, asking) -> standingOn(scope, asked)?.askerPlantId == asking.pet.id }
                ?.first ?: return
        holder.placed.proposal = null
        val cancelled = PetBreedingOut(TOLD_CANCELLED, 0, 0)
        setOf(own.pet.userId, partner.pet.userId).forEach { answer(scope, it, cancelled) }
    }

    private fun agree(
        scope: RoomScope,
        sender: Int,
        own: PlacedPlant,
        partner: PlacedPlant,
    ) {
        val proposal = standingOn(scope, own)?.takeIf { it.askerPlantId == partner.pet.id } ?: return
        own.placed.proposal = null
        val confirmed = PetBreedingOut(TOLD_CONFIRMED, 0, 0)
        setOf(proposal.askerId, sender).forEach { answer(scope, it, confirmed) }
        breed(scope, partner, own)
    }

    /** The proposal on [asked] while it is fresh and its asker and the asker's plant are in the room; else dropped. */
    private fun standingOn(
        scope: RoomScope,
        asked: PlacedPlant,
    ): Proposal? {
        val proposal = asked.placed.proposal ?: return null
        val askerHere = scope.room.units.ofPlayer(proposal.askerId) != null
        val fresh = scope.now() - proposal.askedAt <= ASK_MILLIS
        if (fresh && askerHere && pets.byId(scope, proposal.askerPlantId) != null) return proposal
        asked.placed.proposal = null
        return null
    }

    private fun answer(
        scope: RoomScope,
        playerId: Int,
        message: PetBreedingOut,
    ) = scope.out.send(Lane.Now, Audience.Player(playerId), PetBreedingOutCodec, message)

    fun togglePublic(
        scope: RoomScope,
        playerId: Int,
        petId: Int,
    ) {
        val plant = plant(scope, petId) ?: return
        if (plant.pet.userId != playerId || !plant.pet.isBreedable) return
        plant.pet.allowsBreeding = !plant.pet.allowsBreeding
        saveFlags(plant.pet)
        pets.plantCare.sendStatus(scope, plant.placed, plant.pet)
    }

    private fun breed(
        scope: RoomScope,
        first: PlacedPlant,
        second: PlacedPlant,
    ) {
        if (!first.pet.isBreedable || !second.pet.isBreedable) return
        val pair = listOf(first, second)
        pair.forEach {
            it.placed.proposal = null
            it.pet.breedable = false
            it.pet.allowsBreeding = false
            saveFlags(it.pet)
            pets.plantCare.sendStatus(scope, it.placed, it.pet)
            showBred(scope, it)
        }
        val parents = pair.map { Parent(it.pet.userId, ownerInRoom(scope, it.pet), it.pet.rarity) }
        persist(Runnable { handOutSeeds(first.pet, parents) })
    }

    private data class Parent(
        val ownerId: Int,
        val owner: Player?,
        val rarity: Int,
    )

    private fun showBred(
        scope: RoomScope,
        plant: PlacedPlant,
    ) {
        val unit = pets.unitOf(scope, plant.placed) ?: return
        unit.statuses[PetLines.GESTURE] = PetLines.REBRED
        pets.walking.sendNow(scope, listOf(unit))
        unit.statuses.remove(PetLines.GESTURE)
    }

    /**
     * One seed per owner in the room, unless two owners bred and the lot leaves one of them out. A seed has the
     * rarity of its owner's plant; two rare parents may give rare seeds.
     */
    private fun handOutSeeds(
        plant: MonsterPlant,
        parents: List<Parent>,
    ) {
        val services = plant.services
        val random = services.random()
        parents.forEach { parent ->
            parent.owner?.let {
                PlayerActions.progress(it, CountedAction.BREED_MONSTER_PLANT, 1)
            }
        }
        val growers = parents.filter { it.owner != null }.distinctBy { it.ownerId }
        val owners = growers.mapNotNull { it.owner }
        val lots = seedLots(owners.size > 1, pets.tuning.plants.oneSeedPercent, random)
        val rare = mutates(parents[0].rarity, parents[1].rarity, pets.tuning.plants.rareParentFrom, random)
        val seeds =
            growers
                .filterIndexed { i, _ -> i in lots }
                .map { grower -> handSeed(plant, checkNotNull(grower.owner), grower.rarity, rare) }
                .filter { it !== PetMessages.PlantSeed.NONE }
        val shown = seeds + List(2 - seeds.size) { PetMessages.PlantSeed.NONE }
        val result = PetMessages.plantBreedingResult(shown[0], shown[1])
        owners.mapNotNull { it.client }.forEach { PetMessages.send(it, PetBreedingResultOutCodec, result) }
        val ownerIds = owners.map { it.profile.id }
        PetAchievements.credit(services, PetEvent.SeedsHandedOut(ownerIds, shown[0].userId, shown[1].userId))
    }

    private fun handSeed(
        plant: MonsterPlant,
        owner: Player,
        rarity: Int,
        rare: Boolean,
    ): PetMessages.PlantSeed {
        val services = plant.services
        val seedFurni = MonsterPlantSeeds.definition(services.items(), rare) ?: return PetMessages.PlantSeed.NONE
        val profile = owner.profile
        pets.host.progress(profile.id, GameEvents.PLANT_BRED, BREEDER_POINTS)
        val made = services.items().newFurni(profile.id, seedFurni, 0, 0, rarity.toString())
        val seed = checkNotNull(made) { "seed not created" }
        owner.inventory.furni.put(seed)
        owner.client?.send(AddRoomItem(seed))
        owner.client?.send(FurniListInvalidateOutCodec, FurniListInvalidateOut)
        val base = seed.definition
        return PetMessages.PlantSeed(seed.id, base.spriteId, base.name, profile.id, profile.name, rarity, rare)
    }

    /** Saves the plant's breeding flags off the room lock. */
    fun saveFlags(plant: MonsterPlant) {
        val row = PetStore.PlantBreedFlags(plant.id, plant.breedable, plant.allowsBreeding)
        persist(Runnable { store.savePlantBreeding(row) })
    }

    private fun ownerInRoom(
        scope: RoomScope,
        plant: MonsterPlant,
    ): Player? = pets.host.player(plant.userId)?.takeIf { scope.room.units.ofPlayer(plant.userId) != null }

    private fun plant(
        scope: RoomScope,
        petId: Int,
    ): PlacedPlant? {
        val placed = pets.byId(scope, petId) ?: return null
        val plant = placed.pet as? MonsterPlant ?: return null
        return PlacedPlant(placed, plant)
    }

    private data class PlacedPlant(
        val placed: RoomPet,
        val pet: MonsterPlant,
    )

    companion object {
        /** Two rare parents may give rare seeds, the rarer the pair the likelier (their rarity sum in percent). */
        internal fun mutates(
            rarityOne: Int,
            rarityTwo: Int,
            rareParentFrom: Int,
            random: Random,
        ): Boolean = minOf(rarityOne, rarityTwo) >= rareParentFrom && random.nextInt(PERCENT) < rarityOne + rarityTwo

        /** Parents (0, 1) that get a seed: both, unless two owners bred and the [oneSeedPercent] lot picks one. */
        internal fun seedLots(
            twoOwners: Boolean,
            oneSeedPercent: Int,
            random: Random,
        ): Set<Int> {
            if (!twoOwners || random.nextInt(PERCENT) >= oneSeedPercent) return setOf(0, 1)
            return setOf(random.nextInt(2))
        }

        const val STEP_ASK = 0
        const val STEP_CANCEL = 1
        const val STEP_CONFIRM = 2
        const val TOLD_ASKED = 0
        const val TOLD_CANCELLED = 1
        const val TOLD_CONFIRMED = 2
        const val PERCENT = 100

        const val ASK_MILLIS = 60_000L

        const val BREEDER_POINTS = 5
    }
}
