package zel.protocol.generated

data class CompostPlantIn(
    val petId: Int,
)

object CompostPlantInCodec : IncomingCodec<CompostPlantIn> {
    override val name = "CompostPlantIn"

    override fun read(r: PacketReader): CompostPlantIn = readCompostPlantIn(r)

    override fun write(w: PacketWriter, m: CompostPlantIn) = writeCompostPlantIn(w, m)

    private fun readCompostPlantIn(r: PacketReader): CompostPlantIn {
        val vPetId = r.int()
        return CompostPlantIn(vPetId)
    }

    private fun writeCompostPlantIn(w: PacketWriter, m: CompostPlantIn) {
        w.int(m.petId)
    }
}
