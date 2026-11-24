package zel.room.unit.pet

import zel.game.items.ItemInteraction
import zel.game.pets.Pet
import zel.game.pets.PetSituation
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.state.StateShow
import zel.room.furni.place.Interactions
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitStatus
import zel.room.unit.path.TileState
import zel.room.unit.path.WalkTiles

class PetToys(
    private val pets: RoomPets,
) {
    fun steppedOn(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        furni: RoomFurni,
    ) {
        val pet = placed.pet
        val data = pet.kind ?: return
        val weary = pets.needs.isWeary(pet)
        when (Interactions.of(furni.definition)) {
            NEST -> {
                if ((weary || placed.restingTired) && data.accepts(furni.definition)) {
                    lie(unit, placed)
                }
            }

            TOY -> {
                if (!weary && data.accepts(furni.definition)) play(scope, placed, unit, furni)
            }

            TREE -> {
                if (!weary && data.type == DRAGON) hang(scope, placed, unit, furni)
            }

            TRAMPOLINE -> {
                if (!weary && data.accepts(furni.definition)) jump(scope, placed, unit, furni)
            }
        }
    }

    fun steppedOff(
        scope: RoomScope,
        placed: RoomPet,
        furni: RoomFurni,
    ) {
        if (Interactions.of(furni.definition) !in TOYS) return
        StateShow.whole(scope, furni, REST)
        placed.toyId = null
        val unit = pets.unitOf(scope, placed) ?: return
        if (unit.statuses[PetLines.POSTURE] in setOf(PLAY, HANG, JUMP)) unit.dropStatus(PetLines.POSTURE)
    }

    fun tick(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ) {
        val toyId = placed.toyId ?: return
        if (scope.now() < placed.toyUntil) return
        placed.toyId = null
        val furni = scope.room.furni[toyId]
        if (unit.statuses[PetLines.POSTURE] == JUMP) {
            unit.dropStatus(PetLines.POSTURE)
            randomTile(scope, unit)?.let { pets.walk(scope, placed, unit, it) }
        } else {
            placed.pet.cheer(HAPPINESS)
            unit.dropStatus(PetLines.POSTURE)
        }
        if (furni != null) StateShow.whole(scope, furni, REST)
        unit.setStatus(PetLines.GESTURE, PetLines.SMILE)
        unit.changed = true
    }

    fun goToNest(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ): Boolean {
        val data = placed.pet.kind ?: return false
        val nest =
            scope.room.furni
                .all()
                .filter {
                    Interactions.of(
                        it.definition,
                    ) == NEST && it.anchor != null && data.accepts(it.definition)
                }.randomOrNull(pets.host.random)
                ?.anchor ?: return false
        // A walk to the tile it stands on never steps onto the nest, so a pet already there lies down at once.
        if (unit.tile == nest) lie(unit, placed) else pets.walk(scope, placed, unit, nest)
        return true
    }

    /** False when the room has no toy to go to. */
    fun goToToy(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ): Boolean {
        val data = placed.pet.kind ?: return false
        val toys =
            scope.room.furni
                .all()
                .filter { Interactions.of(it.definition) == TOY && it.anchor != null }
        val toy = toys.filter { data.accepts(it.definition) }.randomOrNull(pets.host.random) ?: toys.firstOrNull()
        val tile = toy?.anchor ?: return false
        pets.walk(scope, placed, unit, tile)
        pets.say(scope, placed, PetSituation.PLAYFUL)
        return true
    }

    fun goToWater(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ): Boolean {
        val water =
            scope.room.furni
                .all()
                .filter { Interactions.of(it.definition) == WATER && it.anchor != null }
                .randomOrNull(pets.host.random)
                ?.anchor ?: return false
        pets.walk(scope, placed, unit, water)
        pets.say(scope, placed, PetSituation.PLAYFUL)
        return true
    }

    fun happyAction(
        pet: Pet,
        unit: RoomUnit,
    ) {
        val actions =
            pet.kind
                ?.happyPostures
                ?.filter { it.isNotBlank() }
                .orEmpty()
        if (actions.isNotEmpty()) {
            unit.setStatus(actions[pets.host.random.nextInt(actions.size)])
            unit.changed = true
        }
    }

    private fun lie(
        unit: RoomUnit,
        placed: RoomPet,
    ) {
        unit.path = unit.path.take(1)
        unit.setStatus(PetLines.POSTURE, UnitStatus.LAY)
        unit.changed = true
        placed.restingTired = true
    }

    private fun play(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        furni: RoomFurni,
    ) {
        unit.path = unit.path.take(1)
        unit.face(furni.direction)
        unit.setStatus(PetLines.POSTURE, PLAY)
        unit.changed = true
        pets.say(scope, placed, PetSituation.PLAYING)
        placed.toyId = furni.id
        val steps = pets.host.random.nextInt(ROUND_STEPS)
        placed.toyUntil = scope.now() + SHORTEST_ROUND_MILLIS + steps * ROUND_STEP_MILLIS
        StateShow.whole(scope, furni, IN_USE)
    }

    private fun hang(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        furni: RoomFurni,
    ) {
        unit.path = unit.path.take(1)
        unit.face(furni.direction)
        unit.setStatus(PetLines.POSTURE, HANG)
        unit.changed = true
        pets.say(scope, placed, PetSituation.PLAYFUL)
        StateShow.whole(scope, furni, IN_USE)
    }

    private fun jump(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        furni: RoomFurni,
    ) {
        unit.setStatus(PetLines.POSTURE, JUMP)
        unit.changed = true
        placed.pet.cheer(HAPPINESS)
        placed.toyId = furni.id
        placed.toyUntil = scope.now() + JUMP_MILLIS
        StateShow.whole(scope, furni, IN_USE)
    }

    private fun randomTile(
        scope: RoomScope,
        unit: RoomUnit,
    ): Point? {
        val floor = scope.room.floor
        return floor
            .tiles()
            .filter { it != unit.tile && floor.isFloor(it) && WalkTiles.at(scope.room, it).state == TileState.Open }
            .randomOrNull(pets.host.random)
    }

    private companion object {
        const val NEST = ItemInteraction.NEST
        const val TOY = ItemInteraction.PET_TOY
        const val TREE = "pet.tree"
        const val TRAMPOLINE = ItemInteraction.PET_TRAMPOLINE
        const val WATER = "water.float"
        val TOYS = setOf(TOY, TREE, TRAMPOLINE)
        const val DRAGON = 12

        const val HAPPINESS = 25
        const val SHORTEST_ROUND_MILLIS = 2_500L
        const val ROUND_STEPS = 20
        const val ROUND_STEP_MILLIS = 500L
        const val JUMP_MILLIS = 4000L
        const val IN_USE = "1"
        const val REST = "0"
        const val PLAY = "pla"
        const val HANG = "hng"
        const val JUMP = "jmp"
    }
}
