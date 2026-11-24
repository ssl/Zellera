package zel.protocol.generated

data class RemoveSaddleFromPetIn(
    val petId: Int,
)

object RemoveSaddleFromPetInCodec : IncomingCodec<RemoveSaddleFromPetIn> {
    override val name = "RemoveSaddleFromPetIn"

    override fun read(r: PacketReader): RemoveSaddleFromPetIn = readRemoveSaddleFromPetIn(r)

    override fun write(w: PacketWriter, m: RemoveSaddleFromPetIn) = writeRemoveSaddleFromPetIn(w, m)

    private fun readRemoveSaddleFromPetIn(r: PacketReader): RemoveSaddleFromPetIn {
        val vPetId = r.int()
        return RemoveSaddleFromPetIn(vPetId)
    }

    private fun writeRemoveSaddleFromPetIn(w: PacketWriter, m: RemoveSaddleFromPetIn) {
        w.int(m.petId)
    }
}
