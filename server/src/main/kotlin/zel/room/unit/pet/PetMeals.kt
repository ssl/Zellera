package zel.room.unit.pet

import zel.game.achievements.GameEvents
import zel.game.items.FurniHeights
import zel.game.items.ItemInteraction
import zel.game.pets.Pet
import zel.game.pets.PetAchievements
import zel.game.pets.PetEvent
import zel.game.pets.PetFood
import zel.game.pets.PetSituation
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.protocol.generated.ObjectDataUpdateOut
import zel.protocol.generated.ObjectDataUpdateOutCodec
import zel.protocol.generated.ObjectRemoveOut
import zel.protocol.generated.ObjectRemoveOutCodec
import zel.protocol.generated.ObjectUpdateOutCodec
import zel.protocol.generated.RoomUnitUpdateOut
import zel.protocol.generated.RoomUnitUpdateOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.state.StateShow
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Interactions
import zel.room.games.GameOut
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.room.unit.path.WalkTiles

class PetMeals(
    private val pets: RoomPets,
    private val store: PetStore,
    private val persist: (Runnable) -> Unit,
) {
    class Meal(
        val furniId: Int,
        val water: Boolean,
        var nextAt: Long,
    ) {
        var bites: Int = 0

        var experience: Int = 0
    }

    fun hasFood(
        scope: RoomScope,
        pet: Pet,
    ): Boolean = bowls(scope, FOOD, pet).isNotEmpty()

    fun goEat(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ) {
        if (!placed.pet.isHungry) return
        goTo(scope, placed, unit, FOOD)
    }

    fun goDrink(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ) = goTo(scope, placed, unit, WATER)

    fun hasWater(
        scope: RoomScope,
        pet: Pet,
    ): Boolean = bowls(scope, WATER, pet).isNotEmpty()

    private fun goTo(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        interaction: String,
    ) {
        val bowl = bowls(scope, interaction, placed.pet).randomOrNull(pets.host.random)?.anchor ?: return
        PetLife.announce(placed, unit, bowl)
    }

    private fun bowls(
        scope: RoomScope,
        interaction: String,
        pet: Pet,
    ): List<RoomFurni> {
        val data = pet.kind ?: return emptyList()
        return scope.room.furni.all().filter { furni ->
            Interactions.of(furni.definition) == interaction && furni.anchor != null && data.accepts(furni.definition)
        }
    }

    fun steppedOn(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        furni: RoomFurni,
    ) {
        if (placed.rider != null || placed.meal != null) return
        when (Interactions.of(furni.definition)) {
            FOOD -> if (wantsFoodFrom(placed.pet, furni)) startEating(scope, placed, unit, furni)
            WATER -> if (wantsWaterFrom(placed.pet, furni)) startDrinking(scope, placed, unit, furni)
        }
    }

    private fun wantsFoodFrom(
        pet: Pet,
        bowl: RoomFurni,
    ): Boolean =
        PetFood.foodLeft(stateOf(bowl), bowl.definition.stateCount) && pet.isHungry &&
            pet.kind?.accepts(bowl.definition) == true

    private fun wantsWaterFrom(
        pet: Pet,
        bowl: RoomFurni,
    ): Boolean =
        stateOf(bowl) < bowl.definition.stateCount - 1 && pet.thirst >= DRINKS_FROM_THIRST &&
            pet.kind?.accepts(bowl.definition) == true

    private fun startEating(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        bowl: RoomFurni,
    ) {
        val heading = unit.bodyDirection
        settle(placed, unit, bowl)
        val from = unit.tile.step((heading + HALF_TURN) % UnitDirection.COUNT)
        val line =
            RoomUnitUpdateOut.List1Item(
                unit.index,
                from.x,
                from.y,
                FurniHeights.formatZ(WalkTiles.at(scope.room, from).height),
                unit.headDirection,
                unit.bodyDirection,
                0,
                "//$EATING",
            )
        val update = RoomUnitUpdateOut(listOf(line))
        scope.out.send(Lane.AfterUnitUpdate, Audience.Room, RoomUnitUpdateOutCodec, update)
        val meal = Meal(bowl.id, water = false, nextAt = 0)
        placed.meal = meal
        bite(scope, placed, meal, bowl, Lane.AfterUnitUpdate)
    }

    private fun startDrinking(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        bowl: RoomFurni,
    ) {
        settle(placed, unit, bowl)
        placed.meal = Meal(bowl.id, water = true, nextAt = scope.now() + NEXT_TICK_MILLIS)
        pets.say(scope, placed, PetSituation.DRINKING, Lane.AfterUnitUpdate)
        pets.host.progress(placed.pet.userId, PetAchievements.ofPet(GameEvents.PET_FED, placed.pet), BITE_POINTS)
    }

    private fun settle(
        placed: RoomPet,
        unit: RoomUnit,
        bowl: RoomFurni,
    ) {
        placed.announcedGoal = null
        placed.next = null
        unit.path = unit.path.take(1)
        unit.face(bowl.direction)
        unit.statuses.remove(PetLines.GESTURE)
        unit.setStatus(PetLines.POSTURE, EATING)
    }

    fun tick(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ): Boolean {
        val meal = placed.meal ?: return false
        if (scope.now() < meal.nextAt) return true
        val bowl = scope.room.furni[meal.furniId]?.takeIf { it.anchor == unit.tile }
        when {
            meal.water -> finishDrinking(scope, placed, bowl)
            bowl != null && wantsFoodFrom(placed.pet, bowl) -> bite(scope, placed, meal, bowl, Lane.Now)
            else -> finishEating(scope, placed, unit, meal)
        }
        return true
    }

    private fun bite(
        scope: RoomScope,
        placed: RoomPet,
        meal: Meal,
        bowl: RoomFurni,
        lane: Lane,
    ) {
        val pet = placed.pet
        val state = stateOf(bowl)
        pet.eat(bowl.definition)
        meal.bites++
        meal.experience += PetFood.biteExperience(bowl.definition)
        meal.nextAt = scope.now() + BITE_MILLIS
        val owner = pet.userId
        if (PetFood.lastBite(state, bowl.definition.stateCount)) {
            persist(Runnable { PlayerActions.progress(owner, CountedAction.PET_EAT, 1) })
            eatUp(scope, bowl, lane)
        } else {
            nextState(scope, bowl, state + 1, lane)
        }
        if (pets.host.random.nextBoolean()) pets.say(scope, placed, PetSituation.EATING, lane)
        persist(Runnable { PetAchievements.credit(pet.services, PetEvent.Fed(pet, BITE_POINTS)) })
    }

    private fun nextState(
        scope: RoomScope,
        bowl: RoomFurni,
        state: Int,
        lane: Lane,
    ) {
        StateShow.set(bowl, state.toString())
        scope.out.send(lane, Audience.Room, ObjectUpdateOutCodec, FurniWire.objectUpdate(bowl))
        bowl.anchor?.let { GameOut.heights(scope, listOf(it)) }
    }

    private fun eatUp(
        scope: RoomScope,
        bowl: RoomFurni,
        lane: Lane,
    ) {
        val tiles = bowl.tiles()
        scope.room.furni.remove(bowl.id) ?: return
        val removal = ObjectRemoveOut(bowl.id.toString(), false, 0, 0, false)
        scope.out.send(lane, Audience.Room, ObjectRemoveOutCodec, removal)
        GameOut.heights(scope, tiles)
        val owner = bowl.placerId
        persist(Runnable { store.deleteFurni(bowl.rowId, owner) })
    }

    private fun finishEating(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        meal: Meal,
    ) {
        placed.meal = null
        unit.statuses.remove(PetLines.POSTURE)
        unit.setStatus(PetLines.GESTURE, PetLines.JOY)
        pets.walking.sendNow(scope, listOf(unit))
        pets.growth.addExperience(scope, placed, meal.experience)
        placed.schedule(RoomPet.Next.ClearGesture, scope.now() + NEXT_TICK_MILLIS)
        placed.nextActionAt = scope.now() + AFTER_MEAL_MILLIS
        pets.save(scope, placed)
    }

    private fun finishDrinking(
        scope: RoomScope,
        placed: RoomPet,
        bowl: RoomFurni?,
    ) {
        placed.meal = null
        placed.pet.changeThirst(-DRINK_AMOUNT)
        if (bowl != null) {
            val state = (stateOf(bowl) + 1).coerceAtMost(maxOf(0, bowl.definition.stateCount - 1))
            StateShow.set(bowl, state.toString())
            val update = ObjectDataUpdateOut(bowl.id.toString(), FurniWire.stuff(bowl))
            scope.out.send(Lane.AfterUnitUpdate, Audience.Room, ObjectDataUpdateOutCodec, update)
            bowl.anchor?.let { GameOut.heights(scope, listOf(it)) }
        }
        placed.schedule(RoomPet.Next.Clear(EATING), scope.now() + NEXT_TICK_MILLIS)
        placed.nextActionAt = scope.now() + AFTER_MEAL_MILLIS
        pets.save(scope, placed)
    }

    private fun stateOf(furni: RoomFurni): Int = furni.state.trim().toIntOrNull() ?: 0

    companion object {
        const val FOOD = ItemInteraction.PET_FOOD
        const val WATER = ItemInteraction.PET_DRINK
        private const val EATING = "eat"
        private const val BITE_MILLIS = 950L

        private const val NEXT_TICK_MILLIS = 450L
        private const val AFTER_MEAL_MILLIS = 18_000L
        private const val BITE_POINTS = 40
        const val DRINKS_FROM_THIRST = 35
        private const val DRINK_AMOUNT = 75
        private const val HALF_TURN = 4
    }
}
