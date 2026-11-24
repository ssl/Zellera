package zel.protocol.generated

data class RemovePetFromFlatIn(
    val petId: Int,
)

object RemovePetFromFlatInCodec : IncomingCodec<RemovePetFromFlatIn> {
    override val name = "RemovePetFromFlatIn"

    override fun read(r: PacketReader): RemovePetFromFlatIn = readRemovePetFromFlatIn(r)

    override fun write(w: PacketWriter, m: RemovePetFromFlatIn) = writeRemovePetFromFlatIn(w, m)

    private fun readRemovePetFromFlatIn(r: PacketReader): RemovePetFromFlatIn {
        val vPetId = r.int()
        return RemovePetFromFlatIn(vPetId)
    }

    private fun writeRemovePetFromFlatIn(w: PacketWriter, m: RemovePetFromFlatIn) {
        w.int(m.petId)
    }
}
