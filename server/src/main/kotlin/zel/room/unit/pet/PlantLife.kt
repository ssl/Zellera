package zel.room.unit.pet

import zel.game.pets.MonsterPlant
import zel.game.pets.PetSituation
import zel.protocol.generated.PetLevelUpdateOut
import zel.protocol.generated.PetLevelUpdateOutCodec
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import kotlin.random.Random

class PlantLife(
    private val pets: RoomPets,
) {
    class Show {
        var postureHidden = false
        var smilingUntil = 0L
        var smileAt = 0L
        var speakAt = 0L
        var busyUntil = 0L
        var graveShown = false

        /** The posture a revive climb shows instead of the clock's stage, until the climb ends. */
        var heldPosture: String? = null

        var composting = false
    }

    fun live(
        scope: RoomScope,
        placed: RoomPet,
        plant: MonsterPlant,
        unit: RoomUnit,
    ) {
        if (plant.isDead) return grave(scope, placed, plant, unit)
        if (plant.showClockStage()) pets.save(scope, placed)
        val now = scope.now()
        val next = placed.next
        if (next != null && now >= placed.dueAt) {
            placed.next = null
            run(scope, placed, plant, unit, next)
        } else if (now >= placed.plant.busyUntil) {
            idle(scope, placed, plant, unit)
        }
        showPosture(unit, plant, placed.plant)
    }

    fun greet(
        scope: RoomScope,
        placed: RoomPet,
        plant: MonsterPlant,
        unit: RoomUnit,
    ) {
        val show = placed.plant
        show.smilingUntil = 0
        show.busyUntil = Long.MAX_VALUE
        show.postureHidden = true
        showPosture(unit, plant, show)
        unit.setStatus(PetLines.GESTURE, PetLines.SMILE)
        placed.schedule(RoomPet.Next.EntryMood(scope.now()), scope.now() + ENTRY_MOOD_MILLIS)
    }

    fun planted(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ) {
        val show = placed.plant
        show.busyUntil = Long.MAX_VALUE
        show.postureHidden = true
        unit.dropStatus(PetLines.POSTURE)
        unit.setStatus(PetLines.GESTURE, PetLines.SMILE)
        pets.walking.sendNow(scope, listOf(unit))
        pets.say(scope, placed, PetSituation.GREET_OWNER)
        placed.schedule(RoomPet.Next.PlantStage, scope.now())
    }

    fun treated(
        scope: RoomScope,
        placed: RoomPet,
        plant: MonsterPlant,
        unit: RoomUnit,
    ) {
        val show = placed.plant
        show.smilingUntil = 0
        show.busyUntil = Long.MAX_VALUE
        show.postureHidden = false
        showPosture(unit, plant, show)
        unit.setStatus(PetLines.GESTURE, PetLines.SMILE)
        pets.walking.sendNow(scope, listOf(unit))
        pets.say(scope, placed, PetSituation.TREATED)
        placed.schedule(RoomPet.Next.AfterSmile, scope.now() + TREAT_SMILE_MILLIS)
    }

    /** Revived: the death stage with the revive gesture, then each stage reached 0.5 s apart; smile ends 5 s later. */
    fun revived(
        scope: RoomScope,
        placed: RoomPet,
        plant: MonsterPlant,
        unit: RoomUnit,
        stageAtDeath: Int,
    ) {
        val show = placed.plant
        placed.next = null
        show.graveShown = false
        show.postureHidden = false
        show.smilingUntil = 0
        show.busyUntil = Long.MAX_VALUE
        show.heldPosture = postureAt(stageAtDeath)
        unit.statuses.clear()
        unit.setStatus(PetLines.POSTURE, postureAt(stageAtDeath))
        unit.setStatus(PetLines.GESTURE, REVIVED)
        pets.walking.sendNow(scope, listOf(unit))
        pets.say(scope, placed, PetSituation.REVIVED)
        placed.schedule(RoomPet.Next.ReviveStage(stageAtDeath + 1, first = true), scope.now() + REVIVE_STEP_MILLIS)
    }

    private fun reviveStage(
        scope: RoomScope,
        placed: RoomPet,
        plant: MonsterPlant,
        unit: RoomUnit,
        next: RoomPet.Next.ReviveStage,
    ) {
        val now = scope.now()
        val show = placed.plant
        if (next.first) pets.plantCare.sendStatus(scope, placed, plant)
        val reached = plant.stage
        if (next.stage <= reached) {
            val update = PetLevelUpdateOut(placed.unitIndex, plant.id, next.stage)
            scope.out.send(Lane.Now, Audience.Room, PetLevelUpdateOutCodec, update)
            show.heldPosture = postureAt(next.stage)
            unit.setStatus(PetLines.POSTURE, postureAt(next.stage))
            unit.setStatus(PetLines.GESTURE, PetLines.SMILE)
            pets.walking.sendNow(scope, listOf(unit))
            pets.say(scope, placed, PetSituation.LEVEL_UP)
        }
        show.busyUntil = Long.MAX_VALUE
        if (next.stage < reached) {
            placed.schedule(RoomPet.Next.ReviveStage(next.stage + 1, first = false), now + REVIVE_STEP_MILLIS)
        } else {
            show.heldPosture = null
            placed.schedule(RoomPet.Next.AfterSmile, now + REVIVE_SMILE_MILLIS)
        }
    }

    /** Rebred: one line with only the gesture, its resting posture in the next tick. */
    fun rebred(
        scope: RoomScope,
        plant: MonsterPlant,
        unit: RoomUnit,
    ) {
        unit.statuses.clear()
        unit.setStatus(PetLines.GESTURE, PetLines.REBRED)
        pets.walking.sendNow(scope, listOf(unit))
        unit.statuses.remove(PetLines.GESTURE)
        unit.setStatus(PetLines.POSTURE, restingPosture(plant))
    }

    fun fertilised(
        scope: RoomScope,
        plant: MonsterPlant,
        unit: RoomUnit,
    ) {
        unit.statuses.clear()
        unit.setStatus(PetLines.POSTURE, restingPosture(plant) + "/" + GROWTH_POSTURE + plant.stage)
        unit.setStatus(PetLines.GESTURE, FERTILISED)
        pets.walking.sendNow(scope, listOf(unit))
        unit.statuses.remove(PetLines.GESTURE)
        unit.setStatus(PetLines.POSTURE, restingPosture(plant))
    }

    private fun run(
        scope: RoomScope,
        placed: RoomPet,
        plant: MonsterPlant,
        unit: RoomUnit,
        next: RoomPet.Next,
    ) {
        val now = scope.now()
        val show = placed.plant
        show.busyUntil = 0
        when {
            next is RoomPet.Next.EntryMood -> {
                unit.setStatus(PetLines.GESTURE, SAD)
                pets.walking.sendNow(scope, listOf(unit))
                pets.say(scope, placed, PetSituation.GENERIC_SAD)
                show.smilingUntil = now + between(SPOKEN_SMILE_MILLIS, pets.host.random)
                placed.schedule(RoomPet.Next.AfterSmile, next.greetedAt + RoomPets.SMILE_MILLIS)
            }

            next is RoomPet.Next.PlantStage -> {
                show.postureHidden = false
                show.busyUntil = Long.MAX_VALUE
                placed.schedule(RoomPet.Next.AfterSmile, now + RoomPets.SMILE_MILLIS)
            }

            next is RoomPet.Next.ReviveStage -> {
                reviveStage(scope, placed, plant, unit, next)
            }

            show.smilingUntil <= now -> {
                endSmile(now, unit, show)
            }
        }
    }

    private fun idle(
        scope: RoomScope,
        placed: RoomPet,
        plant: MonsterPlant,
        unit: RoomUnit,
    ) {
        val now = scope.now()
        val show = placed.plant
        val random = pets.host.random
        if (show.smileAt == 0L) show.smileAt = now + between(QUIET_SMILE_DELAY_MILLIS, random)
        if (show.speakAt == 0L) show.speakAt = now + between(SPEECH_DELAY_MILLIS, random)
        when {
            show.smilingUntil > 0 -> {
                if (now >= show.smilingUntil) endSmile(now, unit, show)
            }

            now >= show.speakAt -> {
                show.speakAt = now + between(SPEECH_DELAY_MILLIS, random)
                show.smilingUntil = now + between(SPOKEN_SMILE_MILLIS, random)
                show.postureHidden = false
                showPosture(unit, plant, show)
                unit.setStatus(PetLines.GESTURE, PetLines.SMILE)
                pets.walking.sendNow(scope, listOf(unit))
                pets.say(scope, placed, PetSituation.GENERIC_HAPPY)
            }

            now >= show.smileAt -> {
                show.smilingUntil = now + between(QUIET_SMILE_MILLIS, random)
                show.postureHidden = true
                unit.setStatus(PetLines.GESTURE, PetLines.SMILE)
            }
        }
    }

    private fun endSmile(
        now: Long,
        unit: RoomUnit,
        show: Show,
    ) {
        show.smilingUntil = 0
        show.postureHidden = false
        unit.dropStatus(PetLines.GESTURE)
        show.smileAt = now + between(QUIET_SMILE_DELAY_MILLIS, pets.host.random)
    }

    private fun grave(
        scope: RoomScope,
        placed: RoomPet,
        plant: MonsterPlant,
        unit: RoomUnit,
    ) {
        if (placed.plant.graveShown) return
        placed.plant.graveShown = true
        placed.next = null
        unit.statuses.clear()
        unit.setStatus(PetLines.POSTURE, GRAVE)
        unit.changed = true
        pets.plantCare.died(plant)
        pets.save(scope, placed)
    }

    /** A draw from [range], both ends included. */
    private fun between(
        range: IntRange,
        random: Random,
    ): Int = range.first + random.nextInt(range.last - range.first + 1)

    companion object {
        private const val SAD = "sad"
        private const val REVIVED = "rev"
        private const val FERTILISED = "spd"
        private const val GRAVE = "rip"
        private const val GROWTH_POSTURE = "grw"
        private const val TREAT_SMILE_MILLIS = 5000L
        private const val REVIVE_STEP_MILLIS = 500L
        private const val REVIVE_SMILE_MILLIS = 5000L
        private const val ENTRY_MOOD_MILLIS = 500L
        private val QUIET_SMILE_DELAY_MILLIS = 2_000..30_000
        private val QUIET_SMILE_MILLIS = 1_000..8_000
        private val SPOKEN_SMILE_MILLIS = 2_000..8_000
        private val SPEECH_DELAY_MILLIS = 11_000..135_000

        fun restingPosture(plant: MonsterPlant): String =
            when {
                plant.isDead -> GRAVE
                plant.isGrown -> PetLines.STAND
                else -> GROWTH_POSTURE + plant.stage
            }

        private fun postureAt(stage: Int): String =
            if (stage >= MonsterPlant.GROWN_STAGE) PetLines.STAND else GROWTH_POSTURE + stage

        /** The growth posture, or none while a smile hides it; a grown plant always stands. */
        private fun showPosture(
            unit: RoomUnit,
            plant: MonsterPlant,
            show: Show,
        ) {
            val held = show.heldPosture
            when {
                held != null -> unit.setStatus(PetLines.POSTURE, held)
                show.postureHidden && !plant.isGrown -> unit.dropStatus(PetLines.POSTURE)
                else -> unit.setStatus(PetLines.POSTURE, restingPosture(plant))
            }
        }
    }
}
