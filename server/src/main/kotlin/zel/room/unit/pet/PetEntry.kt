package zel.room.unit.pet

import zel.game.items.FurniHeights
import zel.game.pets.HorsePet
import zel.game.pets.MonsterPlant
import zel.game.pets.Pet
import zel.game.pets.PetLook
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.RoomUnitsOut
import zel.protocol.outgoing.inventory.InventoryPet
import zel.room.unit.RoomUnit

data class PetEntry(
    val pet: RoomPet,
    val unit: RoomUnit,
    val ownerName: String,
    val ridden: Boolean,
) {
    private fun writeTo(w: PacketWriter) {
        val pet = pet.pet
        val plant = pet as? MonsterPlant
        val flags =
            listOf(
                (pet as? HorsePet)?.saddled == true,
                ridden,
                plant?.isBreedable == true,
                false,
                plant?.isDead == true,
                plant?.allowsBreeding == true,
            )
        w
            .int(pet.id)
            .string(pet.name)
            .string("")
            .string(figureOf(pet))
            .int(unit.index)
            .int(unit.tile.x)
            .int(unit.tile.y)
            .string(FurniHeights.formatZ(unit.z))
            .int(unit.bodyDirection)
            .int(PET_TYPE)
            .int(pet.kind?.type ?: -1)
            .int(pet.userId)
            .string(ownerName)
            .int(plant?.rarity ?: 0)
        flags.forEach(w::boolean)
        w.int(plant?.stage ?: pet.level).string(unit.statuses[PetLines.POSTURE] ?: PetLines.STAND)
    }

    companion object {
        private const val PET_TYPE = 2

        fun roomUnits(entries: List<PetEntry>): RoomUnitsOut {
            val w = PacketWriter().int(entries.size)
            entries.forEach { it.writeTo(w) }
            return RoomUnitsOut(w.toByteArray())
        }

        private fun figureOf(pet: Pet): String {
            if (pet is PetLook) return pet.look
            val data = InventoryPet.of(pet)
            val base = "${data.typeId} ${data.paletteId} ${data.color}"
            if (data.customParts.isEmpty()) return base
            val parts = data.customParts.joinToString(" ") { "${it.partId} ${it.style} ${it.color}" }
            return "$base ${data.customParts.size} $parts"
        }
    }
}
