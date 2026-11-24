package zel.protocol.outgoing.inventory

import zel.game.pets.Pet
import zel.protocol.generated.PetAddedToInventoryOut
import zel.protocol.generated.PetAddedToInventoryOutCodec
import zel.protocol.outgoing.OutgoingMessage

class AddPet(
    val pet: Pet,
    val isOpenInventory: Boolean = false,
) : OutgoingMessage<PetAddedToInventoryOut>(PetAddedToInventoryOutCodec) {
    override fun message(): PetAddedToInventoryOut {
        val data = InventoryPet.of(pet)
        return PetAddedToInventoryOut(
            petId = data.petId,
            name = data.name,
            typeId = data.typeId,
            paletteId = data.paletteId,
            color = data.color,
            breedId = data.breedId,
            customParts =
                data.customParts.map {
                    PetAddedToInventoryOut.CustomPartsItem(it.partId, it.style, it.color)
                },
            level = data.level,
            rarityLevel = data.rarityLevel,
            openInventory = isOpenInventory,
        )
    }
}
