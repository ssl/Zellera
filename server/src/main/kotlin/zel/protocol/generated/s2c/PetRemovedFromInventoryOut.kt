package zel.protocol.generated

data class PetRemovedFromInventoryOut(
    val petId: Int,
)

object PetRemovedFromInventoryOutCodec : OutgoingCodec<PetRemovedFromInventoryOut> {
    override val name = "PetRemovedFromInventoryOut"

    override fun read(r: PacketReader): PetRemovedFromInventoryOut = readPetRemovedFromInventoryOut(r)

    override fun write(w: PacketWriter, m: PetRemovedFromInventoryOut) = writePetRemovedFromInventoryOut(w, m)

    private fun readPetRemovedFromInventoryOut(r: PacketReader): PetRemovedFromInventoryOut {
        val vPetId = r.int()
        return PetRemovedFromInventoryOut(vPetId)
    }

    private fun writePetRemovedFromInventoryOut(w: PacketWriter, m: PetRemovedFromInventoryOut) {
        w.int(m.petId)
    }
}
