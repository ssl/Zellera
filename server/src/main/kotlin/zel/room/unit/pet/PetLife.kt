package zel.room.unit.pet

import zel.game.pets.MonsterPlant
import zel.game.pets.PetSituation
import zel.room.RoomScope
import zel.room.map.Point
import zel.room.tick.Lane
import zel.room.tick.TickPart
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.room.unit.UnitKind
import zel.room.unit.UnitStatus
import zel.room.unit.path.TileState
import zel.room.unit.path.WalkTiles
import zel.util.MILLIS_PER_SECOND
import kotlin.math.abs

class PetLife(
    private val pets: RoomPets,
) {
    private val plants = pets.plants

    val tick: TickPart =
        TickPart { scope ->
            pets.runWalkedBeside(scope)
            pets.nests.tick(scope)
            pets.all(scope).forEach { live(scope, it) }
        }

    fun ownerEntered(
        scope: RoomScope,
        ownerUnit: RoomUnit,
    ) {
        val ownerId = (ownerUnit.kind as? UnitKind.Player)?.playerId ?: return
        val greeting =
            pets.all(scope).filter { it.pet.userId == ownerId && it.rider == null }.mapNotNull { placed ->
                val unit = pets.unitOf(scope, placed) ?: return@mapNotNull null
                val plant = placed.pet as? MonsterPlant
                when {
                    plant?.isDead == true -> return@mapNotNull null
                    plant != null -> plants.greet(scope, placed, plant, unit)
                    else -> greetOwner(scope, placed, unit, ownerUnit)
                }
                placed to unit
            }
        pets.walking.sendNow(scope, greeting.map { it.second })
        greeting.forEach { (placed, _) -> pets.say(scope, placed, PetSituation.GREET_OWNER) }
    }

    private fun greetOwner(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        ownerUnit: RoomUnit,
    ) {
        val now = scope.now()
        placed.announcedGoal = null
        unit.path = emptyList()
        unit.statuses.remove(UnitStatus.MOVE)
        smile(unit, turnTo = ownerUnit.tile)
        val follow =
            if (pets.host.random.nextInt(SECOND_GREETING_ROLLS) < SECOND_GREETINGS_OF_ROLLS) {
                RoomPet.Next.EntrySecondGreeting(now) to ENTRY_SECOND_GREETING_MILLIS
            } else {
                RoomPet.Next.EntryMood(now) to ENTRY_MOOD_MILLIS
            }
        placed.schedule(follow.first, now + follow.second)
        placed.nextChatAt = now + RoomPets.SMILE_MILLIS
    }

    private fun live(
        scope: RoomScope,
        placed: RoomPet,
    ) {
        val unit = pets.unitOf(scope, placed) ?: return
        pets.jumps.tick(scope, placed, unit)
        if (placed.rider != null) return
        val plant = placed.pet as? MonsterPlant
        if (plant != null) plants.live(scope, placed, plant, unit) else livePet(scope, placed, unit)
    }

    private fun livePet(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ) {
        val now = scope.now()
        pets.needs.tick(scope, placed, unit)
        pets.toys.tick(scope, placed, unit)
        val next = placed.next
        val goal = placed.announcedGoal
        when {
            goal != null -> {
                placed.announcedGoal = null
                pets.walk(scope, placed, unit, goal)
            }

            next != null && now >= placed.dueAt -> {
                placed.next = null
                run(scope, placed, unit, next)
            }

            !pets.meals.tick(scope, placed, unit) -> {
                freeTime(scope, placed, unit, now)
            }
        }
    }

    private fun freeTime(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        now: Long,
    ) {
        val playing = pets.football.tick(scope, placed, unit)
        if (unit.isWalking || unit.showsStatus(UnitStatus.MOVE)) return
        if (now >= placed.nextChatAt) {
            idleLine(scope, placed, unit)
            placed.nextChatAt =
                now + (IDLE_CHAT_MIN_SECONDS + pets.host.random.nextInt(IDLE_CHAT_SPREAD + 1)) * MILLIS_PER_SECOND
        }
        val free = !playing && placed.nestId == null && !placed.restingTired
        if (free && placed.next == null && now >= placed.nextActionAt && now >= placed.heldUntil) {
            idleAction(scope, placed, unit)
            placed.nextActionAt =
                now + (ACTION_MIN_SECONDS + pets.host.random.nextInt(ACTION_SPREAD + 1)) * MILLIS_PER_SECOND
        }
    }

    private fun run(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        next: RoomPet.Next,
    ) {
        val now = scope.now()
        when (next) {
            RoomPet.Next.Greet -> {
                val owner = scope.room.units.ofPlayer(placed.pet.userId)
                smile(unit, turnTo = owner?.tile?.takeUnless { unit.statuses.containsKey(UnitStatus.SIT) })
                pets.say(scope, placed, PetSituation.GREET_OWNER, Lane.AfterUnitUpdate)
                placed.schedule(RoomPet.Next.AfterSmile, now + RoomPets.SMILE_MILLIS)
            }

            RoomPet.Next.PlantStage, is RoomPet.Next.ReviveStage -> {
                // Only a plant has it, and plants run in PlantLife.
            }

            RoomPet.Next.AfterSmile -> {
                unit.statuses.remove(PetLines.POSTURE)
                unit.statuses.remove(PetLines.GESTURE)
                val actions = randomActions(placed)
                if (actions.isNotEmpty() && pets.host.random.nextBoolean()) {
                    unit.setStatus(PetLines.POSTURE, actions[pets.host.random.nextInt(actions.size)])
                } else {
                    walkSomewhere(scope, placed, unit)
                }
                placed.nextActionAt = now + ACTION_MIN_SECONDS * MILLIS_PER_SECOND
            }

            RoomPet.Next.EndCommand -> {
                unit.statuses.remove(PetLines.POSTURE)
                unit.setStatus(PetLines.POSTURE, PetLines.STAND)
                unit.setStatus(PetLines.GESTURE, PetLines.SMILE)
                unit.changed = true
                placed.heldUntil = 0
                placed.nextActionAt = now + AFTER_COMMAND_MILLIS
            }

            is RoomPet.Next.Clear -> {
                if (unit.statuses[PetLines.POSTURE] == next.posture) unit.dropStatus(PetLines.POSTURE)
            }

            RoomPet.Next.ClearGesture -> {
                unit.dropStatus(PetLines.GESTURE)
                unit.changed = true
            }

            is RoomPet.Next.EntrySecondGreeting -> {
                pets.walking.sendNow(scope, listOf(unit))
                pets.say(scope, placed, PetSituation.GREET_OWNER)
                placed.schedule(RoomPet.Next.EntryMood(next.greetedAt), now + ENTRY_MOOD_MILLIS - TICK_JITTER_MILLIS)
            }

            is RoomPet.Next.EntryMood -> {
                unit.setStatus(PetLines.GESTURE, MISSED)
                pets.walking.sendNow(scope, listOf(unit))
                pets.say(scope, placed, PetSituation.GENERIC_SAD)
                if (!pets.needs.isWeary(placed.pet) && !seated(scope, unit)) pets.toys.goToToy(scope, placed, unit)
                placed.schedule(RoomPet.Next.AfterSmile, next.greetedAt + RoomPets.SMILE_MILLIS)
            }
        }
    }

    /** The idle line only talks about what the pet wants; the mood tick is what sends it to food, water or rest. */
    private fun idleLine(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ) {
        val pet = placed.pet
        when (pets.needs.of(pet)) {
            PetNeeds.Want.REST -> {
                val fed = pets.meals.hasFood(scope, pet)
                pets.say(scope, placed, if (fed) PetSituation.HUNGRY else PetSituation.TIRED)
            }

            PetNeeds.Want.WATER -> {
                pets.say(scope, placed, PetSituation.THIRSTY)
            }

            PetNeeds.Want.COMFORT -> {
                val playing =
                    scope.now() >= placed.heldUntil && !seated(scope, unit) && pets.toys.goToToy(scope, placed, unit)
                if (!playing) pets.say(scope, placed, PetSituation.GENERIC_SAD)
            }

            PetNeeds.Want.PLAY -> {
                pets.say(scope, placed, PetSituation.GENERIC_HAPPY)
            }

            PetNeeds.Want.NOTHING -> {
                pets.say(scope, placed, PetSituation.GENERIC_NEUTRAL)
            }
        }
    }

    private fun idleAction(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ) {
        val actions = randomActions(placed)
        if (actions.isNotEmpty() && pets.host.random.nextBoolean()) {
            unit.statuses.remove(PetLines.GESTURE)
            unit.setStatus(PetLines.POSTURE, actions[pets.host.random.nextInt(actions.size)])
            unit.changed = true
        } else {
            walkSomewhere(scope, placed, unit)
        }
    }

    private fun walkSomewhere(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ) {
        val goal = freeTile(scope, unit)
        if (goal == null) {
            unit.changed = true
            return
        }
        announce(placed, unit, goal)
    }

    /** A pet on a seat or bed keeps it: it changes posture there but never strolls or goes to a toy on its own. */
    private fun seated(
        scope: RoomScope,
        unit: RoomUnit,
    ): Boolean = WalkTiles.at(scope.room, unit.tile).state.let { it is TileState.Seat || it is TileState.Bed }

    /**
     * A stroll's goal: a random open, free tile of the whole room that the pet has a path to.
     */
    private fun freeTile(
        scope: RoomScope,
        unit: RoomUnit,
    ): Point? {
        val room = scope.room
        if (seated(scope, unit)) return null
        val pool =
            room.floor
                .tiles()
                .filter { tile ->
                    val open = WalkTiles.at(room, tile).state == TileState.Open
                    tile != unit.tile && tile != room.floor.door.tile && open &&
                        room.units.all().none { it.tile == tile }
                }.toMutableList()
        while (pool.isNotEmpty()) {
            val tile = pool.removeAt(pets.host.random.nextInt(pool.size))
            if (pets.walking.stepsTo(scope, unit, tile) != null) return tile
        }
        return null
    }

    fun steppedOn(
        scope: RoomScope,
        unitIndex: Int,
        furniId: Int,
    ) {
        val placed = pets.byIndex(scope, unitIndex) ?: return
        val unit = pets.unitOf(scope, placed) ?: return
        val furni = scope.room.furni[furniId] ?: return
        pets.meals.steppedOn(scope, placed, unit, furni)
        pets.nests.steppedOn(scope, placed, unit, furni)
        pets.toys.steppedOn(scope, placed, unit, furni)
    }

    fun steppedOff(
        scope: RoomScope,
        unitIndex: Int,
        furniId: Int,
    ) {
        val placed = pets.byIndex(scope, unitIndex) ?: return
        val furni = scope.room.furni[furniId] ?: return
        pets.nests.steppedOff(scope, placed, furni)
        pets.toys.steppedOff(scope, placed, furni)
    }

    private fun randomActions(placed: RoomPet): List<String> =
        placed.pet.kind
            ?.idlePostures
            ?.filter { it.isNotBlank() }
            .orEmpty()

    companion object {
        private const val MISSED = "mis"
        private const val ENTRY_MOOD_MILLIS = 500L
        private const val ENTRY_SECOND_GREETING_MILLIS = 300L

        private const val TICK_JITTER_MILLIS = 50L
        private const val SECOND_GREETING_ROLLS = 10
        private const val SECOND_GREETINGS_OF_ROLLS = 6
        private const val AFTER_COMMAND_MILLIS = 10_000L
        private const val IDLE_CHAT_MIN_SECONDS = 3
        private const val IDLE_CHAT_SPREAD = 62
        private const val ACTION_MIN_SECONDS = 3
        private const val ACTION_SPREAD = 12

        fun smile(
            unit: RoomUnit,
            turnTo: Point?,
        ) {
            turnTo?.let { UnitDirection.between(unit.tile, it) }?.let(unit::face)
            unit.statuses.remove(PetLines.POSTURE)
            unit.statuses.remove(PetLines.GESTURE)
            unit.setStatus(PetLines.POSTURE, PetLines.STAND)
            unit.setStatus(PetLines.GESTURE, PetLines.SMILE)
            unit.changed = true
        }

        fun announce(
            placed: RoomPet,
            unit: RoomUnit,
            goal: Point,
        ) {
            unit.statuses.remove(PetLines.POSTURE)
            unit.statuses.remove(PetLines.GESTURE)
            unit.changed = true
            placed.announcedGoal = goal
        }

        fun walkOff(
            scope: RoomScope,
            pets: RoomPets,
            placed: RoomPet,
            unit: RoomUnit,
        ) {
            val room = scope.room
            val goal =
                room.floor
                    .tiles()
                    .filter { WalkTiles.at(room, it).state == TileState.Open && it != unit.tile }
                    .minByOrNull {
                        maxOf(abs(it.x - unit.tile.x), abs(it.y - unit.tile.y)) +
                            pets.host.random.nextDouble()
                    }
                    ?: return
            announce(placed, unit, goal)
            pets.walking.sendNow(scope, listOf(unit))
        }
    }
}
