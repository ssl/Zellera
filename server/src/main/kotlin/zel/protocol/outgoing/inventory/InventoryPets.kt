package zel.protocol.outgoing.inventory

import zel.game.players.Player
import zel.protocol.generated.PetInventoryOut
import zel.protocol.generated.PetInventoryOutCodec
import zel.protocol.outgoing.OutgoingMessage

class InventoryPets(
    val player: Player,
) : OutgoingMessage<PetInventoryOut>(PetInventoryOutCodec) {
    override fun message() =
        PetInventoryOut(
            totalFragments = 1,
            fragmentNo = 0,
            pets =
                player.inventory.pets.all.map {
                    val pet = InventoryPet.of(it)
                    PetInventoryOut.PetsItem(
                        pet.petId,
                        pet.name,
                        pet.typeId,
                        pet.paletteId,
                        pet.color,
                        pet.breedId,
                        pet.customParts.map { part ->
                            PetInventoryOut.PetsItem.CustomPartsItem(part.partId, part.style, part.color)
                        },
                        pet.level,
                        pet.rarityLevel,
                    )
                },
        )
}
