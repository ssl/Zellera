package zel.room.unit.pet

import zel.game.pets.Pet
import zel.game.pets.PetAchievements
import zel.game.pets.PetEvent
import zel.game.pets.PetKind
import zel.game.pets.PetMessages
import zel.game.pets.PetNameLimits
import zel.game.pets.PetPackageNaming
import zel.game.pets.PetSituation
import zel.game.players.Player
import zel.protocol.generated.ConfirmBreedingRequestOutCodec
import zel.protocol.generated.ConfirmBreedingResultOutCodec
import zel.protocol.generated.GoToBreedingNestFailureOutCodec
import zel.protocol.generated.NestBreedingSuccessOutCodec
import zel.protocol.generated.ObjectRemoveOutCodec
import zel.protocol.generated.ObjectUpdateOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.state.StateShow
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Interactions
import zel.room.games.GameOut
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.UnitStatus
import zel.room.unit.path.PathResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

/**
 * Breeding nests. A pet enters one only when its owner sends it; the second pet has to be the nest owner's, who is
 * then asked to name the baby. Agreeing spends the nest, and the baby stands in its place once it is stored.
 */
class PetNests(
    private val pets: RoomPets,
    private val store: PetStore,
    private val persist: (Runnable) -> Unit,
) {
    class Nest(
        val furniId: Int,
    ) {
        /** The pets sitting in it, in the order they came. */
        val sitting = ArrayList<RoomPet>(PAIR)

        /** Its owner has the breeding window open. */
        var asking = false
        var hatch: Hatch? = null

        val isFull: Boolean get() = sitting.size >= PAIR
    }

    class Hatch(
        val nest: RoomFurni,
        val tile: Point,
        /** The spent nest shows its last state until then. */
        val at: Long,
    ) {
        /** Set once the store has answered: the baby, or why there is none. */
        val outcome = AtomicReference<Any?>(null)
    }

    private data object NoBaby

    private data object RoomGone

    /** Sends the pet to a nest it may breed in; else its owner is told why not. */
    fun goBreed(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ): Boolean {
        val nest = nestFor(scope, placed, unit) ?: return false
        val at = nest.anchor ?: return false
        if (unit.tile == at) {
            placed.nestId = nest.id
            sitDown(scope, placed, unit, nest)
            return placed.nestId == nest.id
        }
        pets.walking.letThrough(scope, unit, setOf(nest.id))
        if (pets.walking.walkTo(scope, unit, at) !is PathResult.Found) {
            pets.walking.letThrough(scope, unit, emptySet())
            tell(scope, placed, NO_WAY)
            return false
        }
        unit.statuses.remove(PetLines.POSTURE)
        unit.statuses.remove(PetLines.GESTURE)
        unit.changed = true
        placed.nestId = nest.id
        pets.say(scope, placed, PetSituation.GENERIC_HAPPY)
        return true
    }

    /** The nearest nest that takes the pet, one with a pet waiting before an empty one. */
    private fun nestFor(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ): RoomFurni? {
        val pet = placed.pet
        val all = nestFurni(scope)
        val ofKind = all.filter { suits(scope, it, pet) }
        val withRoom = ofKind.filter { nestOf(scope, it.id)?.let { nest -> nest.isFull || nest.hatch != null } != true }
        val open = withRoom.filter { takes(scope, it, pet) }
        val reason =
            when {
                sitsIn(scope, placed) -> ALREADY_THERE
                pets.needs.isWeary(pet) -> TOO_TIRED
                all.isEmpty() -> NO_NEST
                ofKind.isEmpty() -> NONE_FOR_THIS_PET
                withRoom.isEmpty() -> NEST_FULL
                open.isEmpty() -> SECOND_PET_NOT_THE_OWNERS
                else -> null
            }
        if (reason != null) {
            tell(scope, placed, reason)
            return null
        }
        return open.minWithOrNull(
            compareBy<RoomFurni> { nestOf(scope, it.id)?.sitting.isNullOrEmpty() }.thenBy { steps(it, unit) },
        )
    }

    /** A nest is for the pet types its furni names, for pets that have offspring, and for one type at a time. */
    private fun suits(
        scope: RoomScope,
        furni: RoomFurni,
        pet: Pet,
    ): Boolean {
        val kind = pet.kind ?: return false
        val waiting = nestOf(scope, furni.id)?.sitting?.firstOrNull()
        val sameType = waiting == null || waiting.pet.kind?.type == kind.type
        return kind.offspringType != PetKind.NO_OFFSPRING && kind.accepts(furni.definition) && sameType
    }

    /** Anyone's pet may come first; the second has to be the nest owner's. */
    private fun takes(
        scope: RoomScope,
        furni: RoomFurni,
        pet: Pet,
    ): Boolean = nestOf(scope, furni.id)?.sitting.isNullOrEmpty() || pet.userId == furni.ownerId

    fun steppedOn(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        furni: RoomFurni,
    ) {
        if (!isNest(furni) || placed.nestId != furni.id) return
        unit.path = unit.path.take(1)
        sitDown(scope, placed, unit, furni)
    }

    /** What changed while the pet walked over is checked again; a pet turned away walks off. */
    private fun sitDown(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        furni: RoomFurni,
    ) {
        val nest = pets.nestsIn(scope).getOrPut(furni.id) { Nest(furni.id) }
        if (placed in nest.sitting) return
        val reason =
            when {
                nest.isFull || nest.hatch != null -> NEST_FULL
                !suits(scope, furni, placed.pet) -> NONE_FOR_THIS_PET
                !takes(scope, furni, placed.pet) -> SECOND_PET_NOT_THE_OWNERS
                else -> null
            }
        if (reason != null) {
            placed.nestId = null
            tell(scope, placed, reason)
            PetLife.walkOff(scope, pets, placed, unit)
            return
        }
        nest.sitting += placed
        placed.announcedGoal = null
        if (nest.isFull) ask(scope, nest, furni)
    }

    fun steppedOff(
        scope: RoomScope,
        placed: RoomPet,
        furni: RoomFurni,
    ) {
        if (placed.nestId == furni.id) leave(scope, placed)
    }

    /** The pet is called away or taken out of the room: the other one keeps waiting, an open question is over. */
    fun leave(
        scope: RoomScope,
        placed: RoomPet,
    ) {
        val nestId = placed.nestId ?: return
        placed.nestId = null
        val nest = nestOf(scope, nestId) ?: return
        if (!nest.sitting.remove(placed) || !nest.asking) return
        nest.asking = false
        scope.room.furni[nestId]?.let { showState(scope, it, IDLE, Lane.AfterUnitUpdate) }
    }

    private fun ask(
        scope: RoomScope,
        nest: Nest,
        furni: RoomFurni,
    ) {
        val (one, two) = nest.sitting
        val module = one.pet.pets
        val babyType = one.pet.kind?.offspringType ?: PetKind.NO_OFFSPRING
        val odds =
            PetMessages.BabyOdds(
                babyType,
                module.breedingCurve.chances(averageLevel(one, two)),
                module.catalog.babyBreeds(babyType),
            )
        val request =
            PetMessages.breedingConfirmation(
                furni.id,
                odds,
                PetMessages.BreedingParent(one.pet, one.ownerName),
                PetMessages.BreedingParent(two.pet, two.ownerName),
            )
        nest.asking = true
        showState(scope, furni, ASKING, Lane.AfterUnitUpdate)
        scope.out.send(Lane.AfterUnitUpdate, Audience.Player(furni.ownerId), ConfirmBreedingRequestOutCodec, request)
    }

    /** The nest's owner closed the window: both pets are free again. */
    fun cancel(
        scope: RoomScope,
        player: Player,
        furniId: Int,
    ) {
        val furni = scope.room.furni[furniId]?.takeIf(::isNest) ?: return
        val nest = nestOf(scope, furniId) ?: return
        if (furni.ownerId != player.profile.id || !nest.asking) return
        release(scope, nest, furni)
    }

    /** Every answer of the nest's owner gets a result, which is what closes the window or lets it be tried again. */
    fun confirm(
        scope: RoomScope,
        player: Player,
        furniId: Int,
        rawName: String,
        petOneId: Int,
        petTwoId: Int,
    ) {
        val asker = player.profile.id
        val furni = scope.room.furni[furniId]?.takeIf(::isNest)
        if (furni == null) return answer(scope, asker, furniId, RESULT_NO_NEST)
        if (furni.ownerId != asker) return
        val nest = nestOf(scope, furniId)
        val name = rawName.trim()
        val pair = nest?.takeIf { it.asking }?.sitting?.map { it.pet.id }
        val named = PetPackageNaming.validate(name, PetNameLimits.minimum, PetNameLimits.maximum)
        val result =
            when {
                nest == null || pair?.toSet() != setOf(petOneId, petTwoId) -> RESULT_PETS_MISSING
                named != PetPackageNaming.CLOSE_WIDGET -> RESULT_NAME_INVALID
                spend(scope, nest, furni, name) -> RESULT_OK
                else -> RESULT_NO_NEST
            }
        answer(scope, asker, furniId, result)
    }

    private fun spend(
        scope: RoomScope,
        nest: Nest,
        furni: RoomFurni,
        babyName: String,
    ): Boolean {
        val (one, two) = nest.sitting
        val tile = furni.anchor ?: return false
        scope.room.furni.remove(furni.id) ?: return false
        nest.asking = false
        showState(scope, furni, SPENT, Lane.Now)
        val hatch = Hatch(furni, tile, scope.now() + HATCH_MILLIS)
        nest.hatch = hatch
        val module = one.pet.pets
        val babyType = one.pet.kind?.offspringType ?: PetKind.NO_OFFSPRING
        val level = averageLevel(one, two)
        val breeds = module.catalog.babyBreeds(babyType)
        persist(
            Runnable {
                val spent = store.deleteFurni(furni.rowId, furni.placerId)
                val breed = module.breedingCurve.drawBreed(level, breeds, module.services.random())
                if (breed == null) log.error("no baby of pet type {}: no breeds are listed for it", babyType)
                val baby =
                    breed?.takeIf { spent }?.let { module.maker.ofBreed(babyType, it, babyName, furni.ownerId) }
                if (spent && breed != null && baby == null) log.error("no baby of pet type {}: no data", babyType)
                val kept = hatch.outcome.compareAndSet(null, baby ?: NoBaby)
                if (!kept && baby != null) pets.toInventory(baby, acquired = true)
            },
        )
        return true
    }

    fun tick(scope: RoomScope) {
        val nests = pets.nestsIn(scope)
        nests.values.toList().forEach { nest ->
            val hatch = nest.hatch
            val furni = scope.room.furni[nest.furniId]
            when {
                hatch != null -> hatched(scope, nest, hatch)
                furni == null -> release(scope, nest, null)
                nest.asking && scope.room.units.ofPlayer(furni.ownerId) == null -> release(scope, nest, furni)
            }
            if (nest.hatch == null && nest.sitting.isEmpty()) nests.remove(nest.furniId)
        }
        pets.all(scope).forEach { placed -> if (stoppedShort(scope, placed)) placed.nestId = null }
    }

    /** On its way to a nest but no longer walking: the way got blocked. */
    private fun stoppedShort(
        scope: RoomScope,
        placed: RoomPet,
    ): Boolean {
        if (placed.nestId == null || sitsIn(scope, placed)) return false
        val unit = pets.unitOf(scope, placed) ?: return true
        return !unit.isWalking && !unit.showsStatus(UnitStatus.MOVE)
    }

    private fun hatched(
        scope: RoomScope,
        nest: Nest,
        hatch: Hatch,
    ) {
        val made = hatch.outcome.get() ?: return
        if (scope.now() < hatch.at) return
        nest.hatch = null
        val spent = hatch.nest
        scope.out.send(Lane.Now, Audience.Room, ObjectRemoveOutCodec, FurniWire.objectRemove(spent))
        if (made is Pet) {
            pets.putDownBaby(scope, made, hatch.tile, spent.direction)
            val success = PetMessages.nestSuccess(made.id, made.pets.catalog.tierOf(made))
            pets.toOwner(scope, spent.ownerId, NestBreedingSuccessOutCodec, success)
            persist(Runnable { PetAchievements.credit(made.services, PetEvent.PetAcquired(spent.ownerId, made)) })
        }
        release(scope, nest, null)
    }

    fun unload(nests: Collection<Nest>) {
        nests.mapNotNull { it.hatch }.forEach { hatch ->
            val made = hatch.outcome.getAndSet(RoomGone)
            if (made is Pet) pets.toInventory(made, acquired = true)
        }
    }

    private fun release(
        scope: RoomScope,
        nest: Nest,
        furni: RoomFurni?,
    ) {
        nest.sitting.forEach { placed ->
            placed.nestId = null
            placed.nextActionAt = scope.now()
            pets.unitOf(scope, placed)?.changed = true
        }
        nest.sitting.clear()
        val wasAsking = nest.asking
        nest.asking = false
        if (wasAsking && furni != null) showState(scope, furni, IDLE, Lane.Now)
    }

    private fun sitsIn(
        scope: RoomScope,
        placed: RoomPet,
    ): Boolean =
        placed.nestId
            ?.let { nestOf(scope, it) }
            ?.sitting
            ?.contains(placed) == true

    private fun tell(
        scope: RoomScope,
        placed: RoomPet,
        reason: Int,
    ) {
        val failed = PetMessages.noWayToNest(reason)
        scope.out.send(Lane.Now, Audience.Player(placed.pet.userId), GoToBreedingNestFailureOutCodec, failed)
    }

    private fun answer(
        scope: RoomScope,
        playerId: Int,
        furniId: Int,
        result: Int,
    ) {
        val message = PetMessages.confirmResult(furniId, result)
        scope.out.send(Lane.Now, Audience.Player(playerId), ConfirmBreedingResultOutCodec, message)
    }

    private fun showState(
        scope: RoomScope,
        furni: RoomFurni,
        state: String,
        lane: Lane,
    ) {
        StateShow.set(furni, state)
        scope.out.send(lane, Audience.Room, ObjectUpdateOutCodec, FurniWire.objectUpdate(furni))
        furni.anchor?.let { GameOut.heights(scope, listOf(it)) }
    }

    private fun nestFurni(scope: RoomScope): List<RoomFurni> =
        scope.room.furni
            .all()
            .filter { it.anchor != null && isNest(it) }

    private fun isNest(furni: RoomFurni): Boolean =
        Interactions.of(furni.definition) == BREEDING_NEST && !furni.isBuildPass

    private fun nestOf(
        scope: RoomScope,
        furniId: Int,
    ): Nest? = pets.nestsIn(scope)[furniId]

    private fun steps(
        furni: RoomFurni,
        unit: RoomUnit,
    ): Int = furni.anchor?.let { maxOf(abs(it.x - unit.tile.x), abs(it.y - unit.tile.y)) } ?: Int.MAX_VALUE

    private fun averageLevel(
        one: RoomPet,
        two: RoomPet,
    ): Double = (one.pet.level + two.pet.level) / 2.0

    private companion object {
        val log: Logger = LoggerFactory.getLogger(PetNests::class.java)
        const val BREEDING_NEST = "pet.nest.breeding"
        const val PAIR = 2
        const val HATCH_MILLIS = 1950L

        const val IDLE = "0"
        const val ASKING = "1"
        const val SPENT = "2"

        const val NO_NEST = 0
        const val NONE_FOR_THIS_PET = 1
        const val NEST_FULL = 2
        const val SECOND_PET_NOT_THE_OWNERS = 3
        const val ALREADY_THERE = 4
        const val NO_WAY = 5
        const val TOO_TIRED = 6

        const val RESULT_OK = 0
        const val RESULT_NO_NEST = 1
        const val RESULT_PETS_MISSING = 2
        const val RESULT_NAME_INVALID = 3
    }
}
