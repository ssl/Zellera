package zel.protocol.generated

data class PetAddedToInventoryOut(
    val petId: Int,
    val name: String,
    val typeId: Int,
    val paletteId: Int,
    val color: String,
    val breedId: Int,
    val customParts: List<CustomPartsItem>,
    val level: Int,
    val rarityLevel: Int,
    val openInventory: Boolean,
) {
    data class CustomPartsItem(
        val partId: Int,
        val style: Int,
        val partColor: Int,
    )
}

object PetAddedToInventoryOutCodec : OutgoingCodec<PetAddedToInventoryOut> {
    override val name = "PetAddedToInventoryOut"

    override fun read(r: PacketReader): PetAddedToInventoryOut = readPetAddedToInventoryOut(r)

    override fun write(w: PacketWriter, m: PetAddedToInventoryOut) = writePetAddedToInventoryOut(w, m)

    private fun readPetAddedToInventoryOutCustomPartsItem(r: PacketReader): PetAddedToInventoryOut.CustomPartsItem {
        val vPartId = r.int()
        val vStyle = r.int()
        val vPartColor = r.int()
        return PetAddedToInventoryOut.CustomPartsItem(vPartId, vStyle, vPartColor)
    }

    private fun readPetAddedToInventoryOut(r: PacketReader): PetAddedToInventoryOut {
        val vPetId = r.int()
        val vName = r.string()
        val vTypeId = r.int()
        val vPaletteId = r.int()
        val vColor = r.string()
        val vBreedId = r.int()
        val vCustomParts = List(r.countInt(12)) { readPetAddedToInventoryOutCustomPartsItem(r) }
        val vLevel = r.int()
        val vRarityLevel = r.int()
        val vOpenInventory = r.boolean()
        return PetAddedToInventoryOut(vPetId, vName, vTypeId, vPaletteId, vColor, vBreedId, vCustomParts, vLevel, vRarityLevel, vOpenInventory)
    }

    private fun writePetAddedToInventoryOutCustomPartsItem(w: PacketWriter, m: PetAddedToInventoryOut.CustomPartsItem) {
        w.int(m.partId)
        w.int(m.style)
        w.int(m.partColor)
    }

    private fun writePetAddedToInventoryOut(w: PacketWriter, m: PetAddedToInventoryOut) {
        w.int(m.petId)
        w.string(m.name)
        w.int(m.typeId)
        w.int(m.paletteId)
        w.string(m.color)
        w.int(m.breedId)
        w.countInt(m.customParts.size)
        m.customParts.forEach { writePetAddedToInventoryOutCustomPartsItem(w, it) }
        w.int(m.level)
        w.int(m.rarityLevel)
        w.boolean(m.openInventory)
    }
}
