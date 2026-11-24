package zel.room.unit.pet

import zel.game.pets.PetAchievements
import zel.game.pets.PetEvent
import zel.game.pets.PetLevels
import zel.game.pets.PetMessages
import zel.game.pets.PetSituation
import zel.game.pets.PetStats
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.protocol.generated.PetExperienceOut
import zel.protocol.generated.PetExperienceOutCodec
import zel.protocol.generated.PetLevelNotificationOutCodec
import zel.protocol.generated.PetLevelUpdateOut
import zel.protocol.generated.PetLevelUpdateOutCodec
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane

class PetGrowth(
    private val pets: RoomPets,
    private val persist: (Runnable) -> Unit,
) {
    fun addExperience(
        scope: RoomScope,
        placed: RoomPet,
        amount: Int,
    ) {
        val pet = placed.pet
        pet.experience += amount
        val gained = PetExperienceOut(pet.id, placed.unitIndex, amount)
        scope.out.send(Lane.Now, Audience.Room, PetExperienceOutCodec, gained)
        val reached = PetLevels.levelFor(pet.experience)
        if (reached <= pet.level) return
        pet.level = reached
        pets.answer(scope, placed, PetSituation.LEVEL_UP)
        pet.cheer(PetStats.MAX_HAPPINESS)
        pets.unitOf(scope, placed)?.setStatus(PetLines.GESTURE, LEVEL_UP)
        val update = PetLevelUpdateOut(placed.unitIndex, pet.id, pet.level)
        scope.out.send(Lane.Now, Audience.Room, PetLevelUpdateOutCodec, update)
        val notification = PetMessages.levelNotification(pet)
        scope.out.send(Lane.Now, Audience.Player(pet.userId), PetLevelNotificationOutCodec, notification)
        persist(
            Runnable {
                PetAchievements.credit(pet.services, PetEvent.LevelGained(pet))
                pets.host.player(pet.userId)?.let { PlayerActions.progress(it, CountedAction.PET_LEVEL, 1) }
            },
        )
        pets.save(scope, placed)
    }

    private companion object {
        const val LEVEL_UP = "exp"
    }
}
