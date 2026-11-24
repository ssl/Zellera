package zel.protocol.generated

data class PetInventoryOut(
    val totalFragments: Int,
    val fragmentNo: Int,
    val pets: List<PetsItem>,
) {
    data class PetsItem(
        val petId: Int,
        val name: String,
        val typeId: Int,
        val paletteId: Int,
        val color: String,
        val breedId: Int,
        val customParts: List<CustomPartsItem>,
        val level: Int,
        val rarityLevel: Int,
    ) {
        data class CustomPartsItem(
            val partId: Int,
            val style: Int,
            val partColor: Int,
        )
    }
}

object PetInventoryOutCodec : OutgoingCodec<PetInventoryOut> {
    override val name = "PetInventoryOut"

    override fun read(r: PacketReader): PetInventoryOut = readPetInventoryOut(r)

    override fun write(w: PacketWriter, m: PetInventoryOut) = writePetInventoryOut(w, m)

    private fun readPetInventoryOutPetsItemCustomPartsItem(r: PacketReader): PetInventoryOut.PetsItem.CustomPartsItem {
        val vPartId = r.int()
        val vStyle = r.int()
        val vPartColor = r.int()
        return PetInventoryOut.PetsItem.CustomPartsItem(vPartId, vStyle, vPartColor)
    }

    private fun readPetInventoryOutPetsItem(r: PacketReader): PetInventoryOut.PetsItem {
        val vPetId = r.int()
        val vName = r.string()
        val vTypeId = r.int()
        val vPaletteId = r.int()
        val vColor = r.string()
        val vBreedId = r.int()
        val vCustomParts = List(r.countInt(12)) { readPetInventoryOutPetsItemCustomPartsItem(r) }
        val vLevel = r.int()
        val vRarityLevel = r.int()
        return PetInventoryOut.PetsItem(vPetId, vName, vTypeId, vPaletteId, vColor, vBreedId, vCustomParts, vLevel, vRarityLevel)
    }

    private fun readPetInventoryOut(r: PacketReader): PetInventoryOut {
        val vTotalFragments = r.int()
        val vFragmentNo = r.int()
        val vPets = List(r.countInt(32)) { readPetInventoryOutPetsItem(r) }
        return PetInventoryOut(vTotalFragments, vFragmentNo, vPets)
    }

    private fun writePetInventoryOutPetsItemCustomPartsItem(w: PacketWriter, m: PetInventoryOut.PetsItem.CustomPartsItem) {
        w.int(m.partId)
        w.int(m.style)
        w.int(m.partColor)
    }

    private fun writePetInventoryOutPetsItem(w: PacketWriter, m: PetInventoryOut.PetsItem) {
        w.int(m.petId)
        w.string(m.name)
        w.int(m.typeId)
        w.int(m.paletteId)
        w.string(m.color)
        w.int(m.breedId)
        w.countInt(m.customParts.size)
        m.customParts.forEach { writePetInventoryOutPetsItemCustomPartsItem(w, it) }
        w.int(m.level)
        w.int(m.rarityLevel)
    }

    private fun writePetInventoryOut(w: PacketWriter, m: PetInventoryOut) {
        w.int(m.totalFragments)
        w.int(m.fragmentNo)
        w.countInt(m.pets.size)
        m.pets.forEach { writePetInventoryOutPetsItem(w, it) }
    }
}
