package zel.protocol.outgoing.inventory

import zel.game.pets.MonsterPlant
import zel.game.pets.Pet
import zel.protocol.generated.PacketReader
import zel.protocol.generated.PacketWriter

data class InventoryPet(
    val petId: Int,
    val name: String,
    val typeId: Int,
    val paletteId: Int,
    val color: String,
    val breedId: Int,
    val customParts: List<Part>,
    val level: Int,
    val rarityLevel: Int,
) {
    data class Part(
        val partId: Int,
        val style: Int,
        val color: Int,
    )

    companion object {
        fun of(pet: Pet): InventoryPet {
            val body = PacketWriter().also(pet::writeTo).toByteArray()
            val reader = PacketReader(body)
            val petId = reader.int()
            val name = reader.string()
            val typeId = reader.int()
            val paletteId = reader.int()
            val color = reader.string()
            val breedId = reader.int()
            val parts = List(reader.int()) { Part(reader.int(), reader.int(), reader.int()) }
            val level = reader.int()
            return InventoryPet(
                petId,
                name,
                typeId,
                paletteId,
                color,
                breedId,
                parts,
                level,
                (pet as? MonsterPlant)?.rarity ?: -1,
            )
        }
    }
}
