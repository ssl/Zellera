package zel.room.unit.pet

import zel.game.pets.Pet
import zel.game.pets.PetMood
import zel.game.pets.PetSituation
import zel.room.RoomScope
import zel.room.unit.RoomUnit
import zel.room.unit.UnitStatus
import zel.util.MILLIS_PER_MINUTE

class PetNeeds(
    private val pets: RoomPets,
    private val tuning: PetTuning = pets.tuning,
) {
    /** What a pet wants most; [of] picks the most pressing one. */
    enum class Want(
        val mood: PetMood?,
    ) {
        REST(PetMood.HUNGRY),
        WATER(PetMood.THIRSTY),
        COMFORT(PetMood.SAD),
        PLAY(PetMood.HAPPY),
        NOTHING(null),
    }

    /** Thirst before sadness: a sad pet still shows `gst thr` when thirsty. */
    fun of(pet: Pet): Want =
        with(tuning.moods) {
            when {
                isWeary(pet) -> Want.REST
                pet.happiness >= happyFromHappiness -> Want.PLAY
                pet.thirst >= thirstyFrom -> Want.WATER
                pet.happiness <= sadUpToHappiness -> Want.COMFORT
                else -> Want.NOTHING
            }
        }

    fun isWeary(pet: Pet): Boolean = pet.energy.toLong() * tuning.moods.wearyEnergyDivisor < pet.maxEnergy

    fun isSad(pet: Pet): Boolean = pet.happiness <= tuning.moods.sadUpToHappiness

    fun tick(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ) {
        val now = scope.now()
        if (placed.moodShownAt == 0L) {
            placed.moodShownAt = now
            placed.happinessLostAt = now
        }
        if (placed.sadLineDue) {
            placed.sadLineDue = false
            pets.sayNothing(scope, placed)
        }
        regainEnergy(placed, now)
        growThirsty(placed, now)
        val lying = unit.statuses[PetLines.POSTURE] == UnitStatus.LAY
        if (unit.isWalking || unit.showsStatus(UnitStatus.MOVE)) loseCheer(placed, now)
        val rested = placed.restingTired && placed.pet.energy >= placed.pet.maxEnergy
        if (lying && rested && now >= placed.heldUntil) wakeUp(scope, placed, unit)
        val occupied = placed.next != null || placed.meal != null || placed.nestId != null || now < placed.heldUntil
        dropOldGesture(placed, unit, now)
        if (!occupied && now - placed.moodShownAt >= tuning.moods.everyMillis) {
            placed.moodShownAt = now
            show(scope, placed, unit, of(placed.pet), now)
        }
    }

    /** Food first when there is some, then its nest, else it lies down where it stands. */
    fun seekRest(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ) {
        if (unit.isWalking || unit.statuses[PetLines.POSTURE] == UnitStatus.LAY) return
        val pet = placed.pet
        if (pet.isHungry && pets.meals.hasFood(scope, pet)) return pets.meals.goEat(scope, placed, unit)
        if (pets.toys.goToNest(scope, placed, unit)) return
        unit.setStatus(PetLines.POSTURE, UnitStatus.LAY)
        placed.restingTired = true
        pets.say(scope, placed, PetSituation.SLEEPING)
    }

    private fun regainEnergy(
        placed: RoomPet,
        now: Long,
    ) {
        val pet = placed.pet
        if (pet.energy >= pet.maxEnergy || placed.energyRegainedAt == 0L) {
            placed.energyRegainedAt = now
            return
        }
        val interval = regainIntervalMillis(pet.maxEnergy, tuning.needs.restEnergyDivisor)
        while (now - placed.energyRegainedAt >= interval && pet.energy < pet.maxEnergy) {
            pet.gainEnergy(1)
            placed.energyRegainedAt += interval
        }
    }

    private fun growThirsty(
        placed: RoomPet,
        now: Long,
    ) {
        val steps = stepsSince(placed.thirstGrewAt, now, tuning.needs.thirstMillis)
        if (placed.thirstGrewAt == 0L || steps > 0) placed.thirstGrewAt = now
        if (steps > 0) placed.pet.changeThirst(steps)
    }

    /** Walking wears on a pet's happiness. Lying gives none back: a sad pet stays sad while it lies down. */
    private fun loseCheer(
        placed: RoomPet,
        now: Long,
    ) {
        if (placed.pet.happiness <= 0 || now - placed.happinessLostAt < tuning.needs.walkingSadnessMillis) return
        placed.pet.cheer(-1)
        placed.happinessLostAt = now
    }

    private fun wakeUp(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ) {
        placed.restingTired = false
        unit.dropStatus(PetLines.POSTURE)
        unit.setStatus(PetLines.GESTURE, PetMood.HAPPY.face)
        placed.gestureShownAt = scope.now()
        pets.say(scope, placed, PetSituation.GENERIC_HAPPY)
        placed.nextActionAt = scope.now()
    }

    private fun dropOldGesture(
        placed: RoomPet,
        unit: RoomUnit,
        now: Long,
    ) {
        val since = placed.gestureShownAt ?: return
        if (now - since <= tuning.moods.gestureMillis) return
        placed.gestureShownAt = null
        if (unit.showsStatus(PetLines.GESTURE)) unit.dropStatus(PetLines.GESTURE)
    }

    private fun show(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        want: Want,
        now: Long,
    ) {
        val mood = want.mood ?: return
        unit.setStatus(PetLines.GESTURE, mood.face)
        placed.gestureShownAt = now
        // the sad face's silent line, one tick later: it fires the any-line says boxes, and the pet says nothing
        if (want == Want.COMFORT) placed.sadLineDue = true
        when (want) {
            Want.REST -> seekRest(scope, placed, unit)
            Want.WATER -> pets.meals.goDrink(scope, placed, unit)
            Want.PLAY -> pets.toys.happyAction(placed.pet, unit)
            Want.COMFORT, Want.NOTHING -> Unit
        }
    }

    companion object {
        fun regainIntervalMillis(
            maxEnergy: Int,
            divisor: Int,
        ): Long = MILLIS_PER_MINUTE * divisor / maxEnergy.coerceAtLeast(1)

        /** Whole [interval]s between [since] and [now]; none before the first tick set [since]. */
        fun stepsSince(
            since: Long,
            now: Long,
            interval: Long,
        ): Int = if (since == 0L || interval <= 0) 0 else ((now - since) / interval).toInt()
    }
}
