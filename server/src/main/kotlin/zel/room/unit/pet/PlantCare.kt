package zel.room.unit.pet

import zel.game.achievements.GameEvents
import zel.game.items.Item
import zel.game.pets.MonsterPlant
import zel.game.pets.PetMessages
import zel.game.players.Player
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.protocol.generated.PetRespectNotificationOutCodec
import zel.protocol.generated.PetStatusUpdateOut
import zel.protocol.generated.PetStatusUpdateOutCodec
import zel.protocol.generated.PetSupplementedNotificationOutCodec
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane

class PlantCare(
    private val pets: RoomPets,
    private val store: PetStore,
    private val persist: (Runnable) -> Unit,
) {
    fun treat(
        scope: RoomScope,
        placed: RoomPet,
        plant: MonsterPlant,
        player: Player,
    ) {
        val dead = plant.isDead
        if (!dead && !needsTreat(plant)) return
        PetCare.showHand(scope, player)
        plant.receiveRespect()
        if (!dead) refill(plant)
        scope.out.send(Lane.Now, Audience.Room, PetRespectNotificationOutCodec, PetMessages.respect(plant))
        pets.save(scope, placed)
        if (dead) return
        pets.unitOf(scope, placed)?.let { pets.plants.treated(scope, placed, plant, it) }
        pets.host.progress(player.profile.id, GameEvents.PLANT_TREATED, 1)
        persist(Runnable { PlayerActions.progress(player, CountedAction.TREAT_MONSTER_PLANT, 1) })
    }

    fun supplement(
        scope: RoomScope,
        playerId: Int,
        petId: Int,
        type: Int,
    ) {
        if (type != WATER && type != LIGHT) return
        val placed = pets.byId(scope, petId) ?: return
        val plant = placed.pet as? MonsterPlant ?: return
        if (plant.isDead || plant.userId != playerId) return
        refill(plant)
        plant.cheer(SUPPLEMENT_HAPPINESS)
        sendStatus(scope, placed, plant)
        val notification = PetMessages.supplemented(petId, playerId, type)
        scope.out.send(Lane.Now, Audience.Room, PetSupplementedNotificationOutCodec, notification)
    }

    fun treatmentWith(
        scope: RoomScope,
        playerId: Int,
        petId: Int,
        item: Item,
    ): (() -> Unit)? {
        val placed = pets.byId(scope, petId) ?: return null
        val plant = placed.pet as? MonsterPlant ?: return null
        if (plant.userId != playerId) return null
        return when {
            item.interaction.name == REVIVAL && plant.isDead -> {
                { revive(scope, placed, plant, playerId) }
            }

            item.interaction.name == FERTILISER && !plant.isGrown -> {
                { fertilise(scope, placed, plant) }
            }

            fitsRebreed(item.interaction.name, plant) -> {
                { rebreed(scope, placed, plant) }
            }

            else -> {
                null
            }
        }
    }

    private fun fitsRebreed(
        behaviour: String,
        plant: MonsterPlant,
    ): Boolean {
        if (!plant.isGrown || plant.breedable || plant.isDead) return false
        return when (behaviour) {
            REBREED_COMMON -> plant.rarity <= COMMON_UP_TO
            REBREED_RARE -> plant.rarity in (COMMON_UP_TO + 1)..RARE_UP_TO
            REBREED_RAREST -> plant.rarity > RARE_UP_TO
            else -> false
        }
    }

    private fun rebreed(
        scope: RoomScope,
        placed: RoomPet,
        plant: MonsterPlant,
    ) {
        plant.breedable = true
        pets.breeding.saveFlags(plant)
        pets.unitOf(scope, placed)?.let { pets.plants.rebred(scope, plant, it) }
        sendStatus(scope, placed, plant)
    }

    fun died(plant: MonsterPlant) {
        persist(
            Runnable {
                if (store.markDead(plant.id)) pets.host.progress(plant.userId, GameEvents.PLANT_DIED, 1)
            },
        )
    }

    private fun revive(
        scope: RoomScope,
        placed: RoomPet,
        plant: MonsterPlant,
        playerId: Int,
    ) {
        val stageAtDeath = plant.stage
        plant.restoreLife()
        saveClock(plant)
        val notification = PetMessages.supplemented(plant.id, playerId, REVIVE_SUPPLEMENT)
        scope.out.send(Lane.Now, Audience.Room, PetSupplementedNotificationOutCodec, notification)
        pets.unitOf(scope, placed)?.let { pets.plants.revived(scope, placed, plant, it, stageAtDeath) }
        persist(Runnable { pets.host.progress(playerId, GameEvents.PLANT_REVIVED, 1) })
    }

    private fun fertilise(
        scope: RoomScope,
        placed: RoomPet,
        plant: MonsterPlant,
    ) {
        plant.finishGrowing()
        saveClock(plant)
        pets.unitOf(scope, placed)?.let { pets.plants.fertilised(scope, plant, it) }
        sendStatus(scope, placed, plant)
    }

    private fun refill(plant: MonsterPlant) {
        plant.dieAt(pets.host.nowSeconds() + plant.lifespanSeconds())
        saveClock(plant)
    }

    private fun saveClock(plant: MonsterPlant) {
        val clock = PetStore.PlantClock(plant.id, plant.created, pets.host.nowSeconds() + plant.secondsToLive())
        persist(Runnable { store.savePlant(clock) })
    }

    fun sendStatus(
        scope: RoomScope,
        placed: RoomPet,
        plant: MonsterPlant,
    ) {
        val status =
            PetStatusUpdateOut(
                placed.unitIndex,
                plant.id,
                plant.isBreedable,
                false,
                plant.isDead,
                plant.allowsBreeding,
            )
        scope.out.send(Lane.Now, Audience.Room, PetStatusUpdateOutCodec, status)
    }

    private fun needsTreat(plant: MonsterPlant): Boolean =
        plant.energy.toFloat() / plant.maxEnergy.toFloat() < TREAT_BELOW

    companion object {
        const val COMPOST = "plant.compost"
        const val REVIVAL = "plant.revive"
        const val FERTILISER = "plant.fertilise"
        const val REBREED_COMMON = "plant.rebreed.common"
        const val REBREED_RARE = "plant.rebreed.rare"
        const val REBREED_RAREST = "plant.rebreed.rarest"
        private const val WATER = 0
        private const val LIGHT = 1
        private const val REVIVE_SUPPLEMENT = 2
        private const val SUPPLEMENT_HAPPINESS = 10
        private const val TREAT_BELOW = 0.98f
        private const val COMMON_UP_TO = 5
        private const val RARE_UP_TO = 8
    }
}
