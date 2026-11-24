package zel.room.unit.pet

import zel.game.achievements.GameEvents
import zel.game.effects.EffectRoles
import zel.game.pets.HorsePet
import zel.game.pets.MonsterPlant
import zel.game.pets.Pet
import zel.game.pets.PetAchievements
import zel.game.pets.PetEvent
import zel.game.pets.PetLevels
import zel.game.pets.PetMessages
import zel.game.pets.PetRespectPoints
import zel.game.pets.PetSituation
import zel.game.players.Player
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.protocol.generated.CarryObjectOut
import zel.protocol.generated.CarryObjectOutCodec
import zel.protocol.generated.PetCommandsOutCodec
import zel.protocol.generated.PetInfoOut
import zel.protocol.generated.PetInfoOutCodec
import zel.protocol.generated.PetRespectFailedOut
import zel.protocol.generated.PetRespectFailedOutCodec
import zel.protocol.generated.PetRespectNotificationOutCodec
import zel.protocol.generated.PlayerEffectOut
import zel.protocol.generated.PlayerEffectOutCodec
import zel.room.RoomScope
import zel.room.furni.behaviour.state.RoomControllers
import zel.room.life.RoomLife
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.util.SECONDS_PER_DAY
import kotlin.math.abs

/** Runs under the room's lock. */
class PetCare(
    private val pets: RoomPets,
    private val persist: (Runnable) -> Unit,
) {
    fun info(
        scope: RoomScope,
        playerId: Int,
        petId: Int,
    ) {
        val placed = pets.byId(scope, petId) ?: return
        val pet = placed.pet
        val rideable = pet as? HorsePet
        val owner = pet.userId == playerId
        val riderId =
            placed.rider?.let { scope.room.units[it] }?.let {
                (it.kind as? UnitKind.Player)
                    ?.playerId
            }
        val canRide =
            rideable != null && rideable.saddled && (riderId == null || owner) &&
                (owner || rideable.openToRiders)
        val plant = pet as? MonsterPlant
        val info =
            PetInfoOut(
                int1 = pet.id,
                string1 = pet.name,
                int2 = plant?.stage ?: pet.level,
                int3 = if (plant != null) MonsterPlant.GROWN_STAGE else MAX_LEVEL,
                int4 = pet.experience,
                int5 = plant?.stageGoal() ?: PetLevels.goalAt(pet.level),
                int6 = pet.energy,
                int7 = pet.maxEnergy,
                int8 = pet.happiness,
                int9 = MAX_HAPPINESS,
                int10 = pet.respect,
                int11 = pet.userId,
                int12 = (pets.host.nowSeconds() - pet.created) / SECONDS_PER_DAY,
                string2 = placed.ownerName,
                int13 = pet.breedId,
                boolean1 = canRide,
                boolean2 = riderId == playerId,
                list1 = if (pet is HorsePet) HORSE_SKILL_THRESHOLDS else emptyList(),
                int14 = if (rideable?.openToRiders == true) 1 else 0,
                boolean3 = plant?.isBreedable == true,
                boolean4 = false,
                boolean5 = plant?.isDead == true,
                int15 = plant?.rarity ?: 0,
                int16 = plant?.lifespanSeconds() ?: -1,
                int17 = plant?.secondsToLive() ?: -1,
                int18 = plant?.secondsToGrow() ?: -1,
                boolean6 = plant?.allowsBreeding == true,
            )
        scope.out.send(Lane.Now, Audience.Player(playerId), PetInfoOutCodec, info)
    }

    fun commandList(
        scope: RoomScope,
        playerId: Int,
        petId: Int,
    ) {
        val placed = pets.byId(scope, petId) ?: return
        if (placed.pet.userId != playerId) return
        scope.out.send(
            Lane.Now,
            Audience.Player(playerId),
            PetCommandsOutCodec,
            PetMessages.commands(placed.pet),
        )
    }

    fun scratch(
        scope: RoomScope,
        player: Player,
        petId: Int,
    ) {
        val placed = pets.byId(scope, petId) ?: return
        val unit = scope.room.units.ofPlayer(player.profile.id) ?: return
        if (tooYoung(scope, player)) return
        if (costsPoint(placed.pet) && player.stats.tally.petRespectsLeft <= 0) return
        pets.walkBeside(scope, unit, placed) { later ->
            val reached = pets.byId(later, petId)?.pet ?: return@walkBeside
            if (!costsPoint(reached) || PetRespectPoints.take(player)) scratched(later, player, petId)
        }
    }

    /** Tending a living plant is free; a scratch, and a respect for a dead plant, takes one of the day's points. */
    private fun costsPoint(pet: Pet): Boolean = (pet as? MonsterPlant)?.isDead != false

    fun takeHandItem(
        scope: RoomScope,
        unit: RoomUnit,
        life: RoomLife,
        petId: Int,
    ) {
        val placed = pets.byId(scope, petId) ?: return
        if (life.handItem(unit) == 0 || RoomControllers.handItemsBlocked(scope.room)) return
        pets.walkBeside(scope, unit, placed) { later ->
            if (life.handItem(unit) != 0) {
                life.dropItem(later, unit)
                placed.pet.cheer(HAND_ITEM_HAPPINESS)
                pets.unitOf(later, placed)?.let { pets.toys.happyAction(placed.pet, it) }
            }
        }
    }

    private fun tooYoung(
        scope: RoomScope,
        player: Player,
    ): Boolean {
        val required = pets.host.config(RESPECT_MINIMUM_DAYS_KEY, 0)
        if (required <= 0) return false
        val profile = player.profile
        val age = (pets.host.nowSeconds() - profile.createdAt) / SECONDS_PER_DAY
        if (age >= required) return false
        val failed = PetRespectFailedOut(required, age)
        scope.out.send(Lane.Now, Audience.Player(profile.id), PetRespectFailedOutCodec, failed)
        return true
    }

    private fun scratched(
        scope: RoomScope,
        player: Player,
        petId: Int,
    ) {
        val placed = pets.byId(scope, petId) ?: return
        val pet = placed.pet
        if (pet is MonsterPlant) return pets.plantCare.treat(scope, placed, pet, player)
        showHand(scope, player)
        pet.cheer(SCRATCH_HAPPINESS)
        pet.receiveRespect()
        pets.growth.addExperience(scope, placed, SCRATCH_EXPERIENCE)
        scope.out.send(Lane.Now, Audience.Room, PetRespectNotificationOutCodec, PetMessages.respect(pet))
        showJoy(scope, placed)
        val scratcher = player.profile.id
        pets.host.progress(pet.userId, GameEvents.PET_SCRATCHED_OWN, 1)
        persist(
            Runnable {
                PlayerActions.progress(player, CountedAction.PET_RESPECT, 1)
                PetAchievements.credit(pet.services, PetEvent.Scratched(pet, scratcher))
            },
        )
        pets.save(scope, placed)
    }

    internal fun showJoy(
        scope: RoomScope,
        placed: RoomPet,
    ) {
        val unit = pets.unitOf(scope, placed) ?: return
        unit.statuses.remove(PetLines.POSTURE)
        unit.setStatus(PetLines.GESTURE, PetLines.JOY)
        placed.gestureShownAt = scope.now()
        pets.walking.sendNow(scope, listOf(unit))
        pets.answer(scope, placed, PetSituation.GENERIC_HAPPY)
    }

    /** The rider first walks beside the horse and mounts on arrival; a rider already up gets off. */
    fun mount(
        scope: RoomScope,
        riderUnit: RoomUnit,
        playerId: Int,
        petId: Int,
    ) {
        val ridden = pets.walking.horseOf(scope, riderUnit)
        if (ridden != null) return dismount(scope, riderUnit, ridden)
        val placed = rideable(scope, petId, playerId) ?: return
        val riderIndex = riderUnit.index
        pets.walkBeside(scope, riderUnit, placed) { later ->
            val rider = later.room.units[riderIndex]
            if (rider != null && pets.walking.horseOf(later, rider) == null) mountBeside(later, rider, playerId, petId)
        }
    }

    private fun mountBeside(
        scope: RoomScope,
        riderUnit: RoomUnit,
        playerId: Int,
        petId: Int,
    ) {
        val placed = rideable(scope, petId, playerId) ?: return
        val horseUnit = pets.unitOf(scope, placed) ?: return
        val apart = maxOf(abs(riderUnit.tile.x - horseUnit.tile.x), abs(riderUnit.tile.y - horseUnit.tile.y))
        if (apart > 1) return
        horseUnit.statuses.remove(PetLines.POSTURE)
        horseUnit.statuses.remove(PetLines.GESTURE)
        placed.rider = riderUnit.index
        placed.next = null
        placed.announcedGoal = null
        pets.walking.ride(scope, riderUnit, horseUnit)
        pets.horses.showRiding(scope, placed)
        horseUnit.changed = true
        effect(scope, riderUnit, EffectRoles.of(EffectRoles.RIDING))
        pets.walking.sendNow(scope, listOf(horseUnit, riderUnit))
        persist(Runnable { PetAchievements.credit(placed.pet.services, PetEvent.Mounted(placed.pet, playerId)) })
    }

    private fun rideable(
        scope: RoomScope,
        petId: Int,
        playerId: Int,
    ): RoomPet? =
        pets.byId(scope, petId)?.takeIf { placed ->
            val horse = placed.pet as? HorsePet
            horse != null && placed.rider == null && horse.saddled &&
                (horse.userId == playerId || horse.openToRiders)
        }

    fun toggleRiding(
        scope: RoomScope,
        playerId: Int,
        petId: Int,
    ) {
        val placed = pets.byId(scope, petId) ?: return
        val horse = placed.pet as? HorsePet ?: return
        if (horse.userId != playerId) return
        horse.openToRiders = !horse.openToRiders
        pets.horses.save(horse)
        val rider = placed.rider?.let { scope.room.units[it] } ?: return
        val riderId = (rider.kind as? UnitKind.Player)?.playerId
        if (!horse.openToRiders &&
            riderId != playerId
        ) {
            pets.unitOf(scope, placed)?.let { dismount(scope, rider, it) }
        }
    }

    fun dismount(
        scope: RoomScope,
        riderUnit: RoomUnit,
        horseUnit: RoomUnit,
    ) {
        pets.walking.dismount(scope, riderUnit)
        pets.byIndex(scope, horseUnit.index)?.let { placed ->
            pets.jumps.clear(placed)
            placed.rider = null
            pets.horses.showRiding(scope, placed)
        }
        effect(scope, riderUnit, 0)
        pets.walking.sendNow(scope, listOf(riderUnit, horseUnit))
    }

    fun riderLeft(
        scope: RoomScope,
        riderIndex: Int,
    ) {
        pets.all(scope).filter { it.rider == riderIndex }.forEach { placed ->
            pets.jumps.clear(placed)
            placed.rider = null
            val horseUnit = pets.unitOf(scope, placed) ?: return@forEach
            pets.walking.riderGone(scope, horseUnit)
            pets.horses.showRiding(scope, placed)
            pets.walking.sendNow(scope, listOf(horseUnit))
        }
    }

    private fun effect(
        scope: RoomScope,
        unit: RoomUnit,
        effect: Int,
    ) = scope.out.send(
        Lane.Now,
        Audience.Room,
        PlayerEffectOutCodec,
        PlayerEffectOut(unit.index, effect, 0),
    )

    companion object {
        fun showHand(
            scope: RoomScope,
            player: Player,
        ) {
            val unit = scope.room.units.ofPlayer(player.profile.id) ?: return
            val hand = CarryObjectOut(unit.index, SCRATCH_HAND)
            scope.out.send(Lane.Now, Audience.Room, CarryObjectOutCodec, hand)
        }

        private const val SCRATCH_HAND = 999_999_999
        private const val RESPECT_MINIMUM_DAYS_KEY = "pets.respect.account_age.days"
        private val HORSE_SKILL_THRESHOLDS = listOf(0, 2, 6, 10, 14)
        private const val MAX_LEVEL = 20
        private const val MAX_HAPPINESS = 100
        private const val SCRATCH_HAPPINESS = 15
        private const val HAND_ITEM_HAPPINESS = 10
        private const val SCRATCH_EXPERIENCE = 10
    }
}
