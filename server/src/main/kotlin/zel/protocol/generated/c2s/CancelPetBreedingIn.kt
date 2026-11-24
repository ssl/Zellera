package zel.protocol.generated

data class CancelPetBreedingIn(
    val itemId: Int,
)

object CancelPetBreedingInCodec : IncomingCodec<CancelPetBreedingIn> {
    override val name = "CancelPetBreedingIn"

    override fun read(r: PacketReader): CancelPetBreedingIn = readCancelPetBreedingIn(r)

    override fun write(w: PacketWriter, m: CancelPetBreedingIn) = writeCancelPetBreedingIn(w, m)

    private fun readCancelPetBreedingIn(r: PacketReader): CancelPetBreedingIn {
        val vItemId = r.int()
        return CancelPetBreedingIn(vItemId)
    }

    private fun writeCancelPetBreedingIn(w: PacketWriter, m: CancelPetBreedingIn) {
        w.int(m.itemId)
    }
}
