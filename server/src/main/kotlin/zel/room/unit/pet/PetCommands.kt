package zel.room.unit.pet

import zel.game.pets.Pet
import zel.game.pets.PetCommand
import zel.game.pets.PetMood
import zel.game.pets.PetSituation
import zel.room.RoomScope
import zel.room.map.Point
import zel.room.tick.TickPart
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.room.unit.UnitKind
import zel.room.unit.UnitStatus
import zel.room.unit.path.TileState
import zel.room.unit.path.WalkTiles

class PetCommands(
    private val pets: RoomPets,
    private val care: PetCare,
    private val rules: PetTuning.Obedience = pets.tuning.obedience,
) {
    private sealed interface Act {
        data object Release : Act

        data object ComeToOwner : Act

        data object Stay : Act

        data object Hush : Act

        data object Speak : Act

        data object Eat : Act

        data object Drink : Act

        data object Kick : Act

        data object Breed : Act

        /** On its feet: ends a sit, a lie-down or a stay; a rider stays in the saddle. */
        data object Stand : Act

        data object Nest : Act

        data object StepForward : Act

        /** A turn on the spot by [steps] of 45 degrees, clockwise when positive. */
        data class Turn(
            val steps: Int,
        ) : Act

        data object Dip : Act

        /** Sit or lie down and hold it; a refusal still shows the posture. */
        data class Hold(
            val posture: String,
            val millis: Long,
        ) : Act

        data class Trick(
            val status: String?,
            val millis: Long,
        ) : Act
    }

    fun heard(
        scope: RoomScope,
        speaker: RoomUnit,
        text: String,
    ) {
        val ownerId = (speaker.kind as? UnitKind.Player)?.playerId ?: return
        pets
            .all(scope)
            .filter { it.pet.userId == ownerId }
            .forEach { placed ->
                commandIn(placed, text)?.let { placed.heard = RoomPet.Heard(it, speaker.index) }
            }
    }

    private fun commandIn(
        placed: RoomPet,
        text: String,
    ): PetCommand? {
        val prefix = placed.pet.name + " "
        if (!text.startsWith(prefix, ignoreCase = true)) return null
        val word = text.substring(prefix.length).trim()
        return placed.pet.kind
            ?.commands
            ?.find { it.word.equals(word, ignoreCase = true) }
    }

    val tick: TickPart =
        TickPart { scope ->
            pets.all(scope).forEach { placed ->
                val heard = placed.heard
                placed.heard = null
                val owner = heard?.let { scope.room.units[it.ownerIndex] }
                val unit = owner?.let { pets.unitOf(scope, placed) }
                if (heard != null && owner != null && unit != null) consider(scope, placed, unit, owner, heard.command)
            }
        }

    /**
     * The owner's word puts a rider down whatever the pet then does. A command that comes too soon after the last, or
     * the same one too often, is not heard. An enabled command is then followed or refused by lot; what it lacks to
     * carry the act out (food, water, a nest) is a refusal too.
     */
    private fun consider(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        owner: RoomUnit,
        command: PetCommand,
    ) {
        val act = actOf(command.id)
        if (command.id !in RIDER_STAYS_ON) {
            placed.rider?.let { scope.room.units[it] }?.let { care.dismount(scope, it, unit) }
        }
        pets.football.stop(placed)
        placed.restingTired = false
        val pet = placed.pet
        val puzzles = pet.level < command.level || (act == Act.Kick && !pets.football.hasBall(scope))
        if (!puzzles && !paced(placed, command.id, scope.now())) {
            val nagged = placed.sameCommandCount > rules.repeatsAllowed
            if (nagged && pets.host.random.nextInt(rules.grumbleOneIn) == 0) {
                pets.answer(scope, placed, PetSituation.DISOBEY)
            }
            return
        }
        if (act != Act.Breed) pets.nests.leave(scope, placed)
        // the answer has a face of its own, which the timer of an older mood must not take away
        placed.gestureShownAt = null
        when {
            puzzles -> {
                puzzled(scope, placed, unit)
            }

            act == Act.Release -> {
                perform(scope, placed, unit, owner, act)
            }

            pet.happiness < rules.sulksBelowHappiness -> {
                refuse(scope, placed, unit, PetSituation.GENERIC_SAD)
            }

            !obeys(pet) -> {
                refuse(scope, placed, unit, if (isDrained(pet)) PetSituation.HUNGRY else PetSituation.DISOBEY)
            }

            perform(scope, placed, unit, owner, act) -> {
                pay(scope, placed, unit, command)
            }

            else -> {
                refuse(scope, placed, unit, PetSituation.DISOBEY)
            }
        }
    }

    private fun paced(
        placed: RoomPet,
        commandId: Int,
        now: Long,
    ): Boolean {
        val sinceLast = now - placed.lastCommandAt
        if (sinceLast < rules.gapMillis) return false
        if (sinceLast > rules.repeatWindowMillis) placed.sameCommandCount = 0
        placed.sameCommandCount = if (commandId == placed.lastCommandId) placed.sameCommandCount + 1 else 1
        if (placed.sameCommandCount > rules.repeatsAllowed) return false
        placed.lastCommandId = commandId
        placed.lastCommandAt = now
        return true
    }

    /** A drained pet blames its empty stomach and shows it. */
    private fun refuse(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        line: PetSituation,
    ) {
        halt(placed, unit)
        unit.setStatus(PetLines.POSTURE, REFUSED_POSTURE)
        if (line == PetSituation.HUNGRY) {
            unit.setStatus(PetLines.GESTURE, PetMood.HUNGRY.face)
        } else {
            unit.dropStatus(PetLines.GESTURE)
        }
        pets.answer(scope, placed, line)
        holdFor(scope, placed, REFUSED_HOLD_MILLIS)
    }

    private fun obeys(pet: Pet): Boolean {
        val percent = if (isDrained(pet)) rules.drainedPercent else rules.restedPercent
        return pets.host.random.nextInt(PERCENT) < percent
    }

    private fun isDrained(pet: Pet): Boolean = pet.energy.toLong() * rules.drainedEnergyDivisor < pet.maxEnergy

    /** A followed command costs as much energy as the experience it gives. */
    private fun pay(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        command: PetCommand,
    ) {
        val pet = placed.pet
        pet.energy = (pet.energy - command.xp).coerceAtLeast(0)
        if (command.xp > 0) pets.growth.addExperience(scope, placed, command.xp)
        unit.changed = true
    }

    /** Carries the act out; false when the pet could not (no food, no water, no ball, no nest). */
    private fun perform(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        owner: RoomUnit,
        act: Act,
    ): Boolean =
        when (act) {
            Act.Release -> {
                placed.heldUntil = 0
                placed.next = null
                unit.statuses.remove(PetLines.POSTURE)
                unit.statuses.remove(PetLines.GESTURE)
                true
            }

            Act.Hush -> {
                pets.answer(scope, placed, PetSituation.MUTED)
                placed.muted = true
                true
            }

            Act.Speak -> {
                placed.muted = false
                pets.answer(scope, placed, cheerful(placed, CHEERFUL_SPEAKING))
                true
            }

            Act.Stand -> {
                stand(scope, placed, unit)
            }

            is Act.Turn -> {
                halt(placed, unit)
                unit.face(Math.floorMod(unit.bodyDirection + act.steps, UnitDirection.COUNT))
                pets.walking.sendNow(scope, listOf(unit))
                true
            }

            is Act.Hold -> {
                halt(placed, unit)
                unit.setStatus(PetLines.POSTURE, act.posture)
                unit.setStatus(PetLines.GESTURE, PetLines.SMILE)
                holdFor(scope, placed, act.millis)
                true
            }

            is Act.Trick -> {
                act.status?.let {
                    halt(placed, unit)
                    unit.setStatus(PetLines.POSTURE, it)
                    placed.schedule(RoomPet.Next.Clear(it), scope.now() + act.millis)
                }
                pets.answer(scope, placed, cheerful(placed, CHEERFUL_TRICK))
                true
            }

            else -> {
                go(scope, placed, unit, owner, act)
            }
        }

    private fun go(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        owner: RoomUnit,
        act: Act,
    ): Boolean =
        when (act) {
            Act.ComeToOwner -> {
                freeTileBeside(scope, owner)?.let { PetLife.announce(placed, unit, it) }
                pets.answer(scope, placed, cheerful(placed, CHEERFUL_COMING))
                true
            }

            Act.Stay -> {
                halt(placed, unit)
                placed.heldUntil = scope.now() + rules.stayMillis
                pets.answer(scope, placed, PetSituation.GENERIC_NEUTRAL)
                true
            }

            Act.Eat -> {
                goEat(scope, placed, unit)
            }

            Act.Drink -> {
                goDrink(scope, placed, unit)
            }

            Act.Kick -> {
                pets.football.start(scope, placed, unit)
            }

            Act.Breed -> {
                halt(placed, unit)
                pets.nests.goBreed(scope, placed, unit)
            }

            Act.Nest -> {
                goToNest(scope, placed, unit)
            }

            Act.StepForward -> {
                stepForward(scope, placed, unit)
            }

            Act.Dip -> {
                pets.toys.goToWater(scope, placed, unit)
            }

            else -> {
                false
            }
        }

    private fun stand(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ): Boolean {
        halt(placed, unit)
        placed.heldUntil = 0
        unit.setStatus(PetLines.POSTURE, PetLines.STAND)
        unit.dropStatus(PetLines.GESTURE)
        placed.schedule(RoomPet.Next.Clear(PetLines.STAND), scope.now() + TRICK_MILLIS)
        return true
    }

    /** In its nest a pet naps as long as a stay lasts and then until it is rested. */
    private fun goToNest(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ): Boolean {
        val going = pets.toys.goToNest(scope, placed, unit)
        if (going) {
            placed.restingTired = true
            placed.heldUntil = scope.now() + rules.stayMillis
            pets.answer(scope, placed, PetSituation.SLEEPING)
        }
        return going
    }

    private fun stepForward(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ): Boolean {
        val ahead = unit.tile.step(unit.bodyDirection)
        val open = WalkTiles.at(scope.room, ahead).state == TileState.Open
        if (open) pets.walk(scope, placed, unit, ahead)
        return open
    }

    /** A pet eats on command once it is down to half its energy. */
    private fun goEat(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ): Boolean {
        val pet = placed.pet
        if (pet.energy * 2 > pet.maxEnergy || !pets.meals.hasFood(scope, pet)) return false
        pets.answer(scope, placed, PetSituation.HUNGRY)
        pets.meals.goEat(scope, placed, unit)
        return true
    }

    private fun goDrink(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ): Boolean {
        val pet = placed.pet
        if (pet.thirst < PetMeals.DRINKS_FROM_THIRST || !pets.meals.hasWater(scope, pet)) return false
        pets.answer(scope, placed, PetSituation.THIRSTY)
        pets.meals.goDrink(scope, placed, unit)
        return true
    }

    private fun puzzled(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ) {
        halt(placed, unit)
        unit.statuses.remove(PetLines.POSTURE)
        unit.setStatus(PetLines.GESTURE, PUZZLED)
        unit.changed = true
        pets.answer(scope, placed, PetSituation.UNKNOWN_COMMAND)
        holdFor(scope, placed, PUZZLED_MILLIS)
    }

    private fun holdFor(
        scope: RoomScope,
        placed: RoomPet,
        millis: Long,
    ) {
        val until = scope.now() + millis
        placed.heldUntil = until
        placed.schedule(RoomPet.Next.EndCommand, until)
    }

    private fun halt(
        placed: RoomPet,
        unit: RoomUnit,
    ) {
        placed.announcedGoal = null
        unit.path = emptyList()
        unit.dropStatus(UnitStatus.MOVE)
    }

    private fun cheerful(
        placed: RoomPet,
        above: Int,
    ): PetSituation = if (placed.pet.happiness > above) PetSituation.PLAYFUL else PetSituation.GENERIC_NEUTRAL

    private fun freeTileBeside(
        scope: RoomScope,
        owner: RoomUnit,
    ): Point? =
        generateSequence(0) { it + 1 }
            .take(UnitDirection.COUNT)
            .map { owner.tile.step(owner.bodyDirection + it) }
            .firstOrNull { WalkTiles.at(scope.room, it).state == TileState.Open }

    private companion object {
        const val PERCENT = 100
        const val PUZZLED = "puz"
        const val CHEERFUL_COMING = 75
        const val CHEERFUL_SPEAKING = 50
        const val CHEERFUL_TRICK = 60
        const val PUZZLED_MILLIS = 2_500L
        const val REFUSED_HOLD_MILLIS = 4_500L
        const val REFUSED_POSTURE = UnitStatus.SIT
        const val TRICK_MILLIS = 500L
        const val HOLD_MILLIS = 4_000L
        const val QUARTER_TURN = 2

        /** Sit, stand and jump keep a rider in the saddle; every other command drops them. */
        val RIDER_STAYS_ON = setOf(1, 8, 9)

        private val ACTS: Map<Int, Act> =
            buildMap {
                put(0, Act.Release)
                put(1, Act.Hold(UnitStatus.SIT, HOLD_MILLIS))
                put(2, Act.Hold(UnitStatus.LAY, HOLD_MILLIS))
                listOf(3, 7, 15, 16, 18).forEach { put(it, Act.ComeToOwner) }
                put(6, Act.Stay)
                put(8, Act.Stand)
                put(10, Act.Speak)
                put(12, Act.Hush)
                put(13, Act.Nest)
                put(14, Act.Drink)
                put(17, Act.Kick)
                put(24, Act.StepForward)
                put(25, Act.Turn(-QUARTER_TURN))
                put(26, Act.Turn(QUARTER_TURN))
                put(29, Act.Dip)
                put(43, Act.Eat)
                put(46, Act.Breed)
                trick(4, "beg", TRICK_MILLIS)
                trick(5, "ded", HOLD_MILLIS)
                trick(9, "jmp", 2_000L)
                trick(11, null, 4_000L)
                trick(19, "jmp", 3_000L)
                trick(20, "flt", 3_000L)
                trick(21, "dan", 5_000L)
                trick(22, null, 2_000L)
                trick(23, null, 1_000L)
                trick(27, null, 4_000L)
                trick(28, null, 2_000L)
                trick(30, "wav", TRICK_MILLIS)
                trick(31, "dan", 5_000L)
                trick(32, "jmp", 3_000L)
                trick(33, "dan", 4_000L)
                trick(34, "jmp", 4_000L)
                trick(35, "wng", 2_500L)
                trick(36, "flm", 1_000L)
                trick(37, null, 4_000L)
                trick(38, "eat", 1_000L)
                trick(40, null, 4_000L)
                trick(41, null, 3_000L)
                trick(42, null, 4_000L)
                trick(44, null, 2_000L)
                trick(45, null, 3_000L)
            }

        private fun MutableMap<Int, Act>.trick(
            id: Int,
            status: String?,
            millis: Long,
        ) {
            put(id, Act.Trick(status, millis))
        }

        fun actOf(id: Int): Act = ACTS[id] ?: Act.Trick(null, TRICK_MILLIS)
    }
}
