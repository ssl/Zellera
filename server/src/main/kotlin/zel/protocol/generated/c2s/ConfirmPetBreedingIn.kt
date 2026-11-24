package zel.protocol.generated

data class ConfirmPetBreedingIn(
    val itemId: Int,
    val name: String,
    val petOneId: Int,
    val petTwoId: Int,
)

object ConfirmPetBreedingInCodec : IncomingCodec<ConfirmPetBreedingIn> {
    override val name = "ConfirmPetBreedingIn"

    override fun read(r: PacketReader): ConfirmPetBreedingIn = readConfirmPetBreedingIn(r)

    override fun write(w: PacketWriter, m: ConfirmPetBreedingIn) = writeConfirmPetBreedingIn(w, m)

    private fun readConfirmPetBreedingIn(r: PacketReader): ConfirmPetBreedingIn {
        val vItemId = r.int()
        val vName = r.string()
        val vPetOneId = r.int()
        val vPetTwoId = r.int()
        return ConfirmPetBreedingIn(vItemId, vName, vPetOneId, vPetTwoId)
    }

    private fun writeConfirmPetBreedingIn(w: PacketWriter, m: ConfirmPetBreedingIn) {
        w.int(m.itemId)
        w.string(m.name)
        w.int(m.petOneId)
        w.int(m.petTwoId)
    }
}
