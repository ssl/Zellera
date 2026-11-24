package zel.protocol.generated

data class CustomizePetWithFurniIn(
    val itemId: Int,
    val petId: Int,
)

object CustomizePetWithFurniInCodec : IncomingCodec<CustomizePetWithFurniIn> {
    override val name = "CustomizePetWithFurniIn"

    override fun read(r: PacketReader): CustomizePetWithFurniIn = readCustomizePetWithFurniIn(r)

    override fun write(w: PacketWriter, m: CustomizePetWithFurniIn) = writeCustomizePetWithFurniIn(w, m)

    private fun readCustomizePetWithFurniIn(r: PacketReader): CustomizePetWithFurniIn {
        val vItemId = r.int()
        val vPetId = r.int()
        return CustomizePetWithFurniIn(vItemId, vPetId)
    }

    private fun writeCustomizePetWithFurniIn(w: PacketWriter, m: CustomizePetWithFurniIn) {
        w.int(m.itemId)
        w.int(m.petId)
    }
}
