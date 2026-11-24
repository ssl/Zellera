package zel.room.unit.pet

import zel.game.items.Item
import zel.game.pets.HorsePet
import zel.game.players.Player
import zel.protocol.generated.PetFigureUpdateOut
import zel.protocol.generated.PetFigureUpdateOutCodec
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.pet.PetStore.HorseLook

class HorseCare(
    private val pets: RoomPets,
    private val store: PetStore,
    private val persist: (Runnable) -> Unit,
) {
    fun use(
        scope: RoomScope,
        playerId: Int,
        petId: Int,
        item: Item,
    ): (() -> Unit)? {
        val (placed, horse) = ownHorse(scope, petId, playerId) ?: return null
        val look = HorseGrooming.groomed(lookOf(horse), item.params, item.id) ?: return null
        return {
            restyle(scope, placed, horse, look)
            saveLook(look)
        }
    }

    fun showRiding(
        scope: RoomScope,
        placed: RoomPet,
    ) {
        val horse = placed.pet as? HorsePet ?: return
        showFigure(scope, placed, horse, lookOf(horse))
    }

    fun removeSaddle(
        scope: RoomScope,
        player: Player,
        petId: Int,
    ) {
        val (placed, horse) = ownHorse(scope, petId, player.profile.id) ?: return
        val look = lookOf(horse).takeIf { it.saddle } ?: return
        val saddle = pets.host.saddle(look.saddleDefinitionId) ?: return
        val bare = look.copy(saddle = false)
        restyle(scope, placed, horse, bare)
        val playerId = player.profile.id
        persist(
            Runnable {
                store
                    .saveHorse(bare) { connection -> pets.host.newFurni(connection, playerId, saddle) }
                    ?.let(player.inventory.furni::deliver)
            },
        )
    }

    /** Saves the horse's figure and riding permission off the room lock. */
    fun save(horse: HorsePet) = saveLook(lookOf(horse))

    private fun saveLook(look: HorseLook) = persist(Runnable { store.saveHorse(look) })

    private fun ownHorse(
        scope: RoomScope,
        petId: Int,
        ownerId: Int,
    ): Pair<RoomPet, HorsePet>? {
        val placed = pets.byId(scope, petId) ?: return null
        val horse = placed.pet as? HorsePet ?: return null
        return if (horse.userId == ownerId) placed to horse else null
    }

    private fun lookOf(horse: HorsePet) =
        HorseLook(
            id = horse.id,
            palette = horse.race,
            hairStyle = horse.mane.style,
            hairColor = horse.mane.tint,
            saddle = horse.saddled,
            anyoneRides = horse.openToRiders,
            saddleDefinitionId = horse.saddleDefinitionId,
        )

    private fun restyle(
        scope: RoomScope,
        placed: RoomPet,
        horse: HorsePet,
        look: HorseLook,
    ) {
        horse.race = look.palette
        horse.mane = HorsePet.Mane(look.hairStyle, look.hairColor)
        horse.saddled = look.saddle
        horse.saddleDefinitionId = look.saddleDefinitionId
        showFigure(scope, placed, horse, look)
    }

    private fun showFigure(
        scope: RoomScope,
        placed: RoomPet,
        horse: HorsePet,
        look: HorseLook,
    ) {
        val hair = listOf(MANE, TAIL).map { PetFigureUpdateOut.List1Item(it, look.hairStyle, look.hairColor) }
        val saddle =
            if (look.saddle) listOf(PetFigureUpdateOut.List1Item(SADDLE, horse.saddleFigurePart, 0)) else emptyList()
        val figure =
            PetFigureUpdateOut(
                int1 = placed.unitIndex,
                int2 = look.id,
                int3 = horse.kind?.type ?: -1,
                int4 = look.palette,
                string1 = horse.figureColor,
                int5 = HorsePet.breedOfCoat(look.palette),
                list1 = hair + saddle,
                boolean1 = look.saddle,
                boolean2 = placed.rider != null,
            )
        scope.out.send(Lane.Now, Audience.Room, PetFigureUpdateOutCodec, figure)
    }

    private companion object {
        const val MANE = 2
        const val TAIL = 3
        const val SADDLE = 4
    }
}
