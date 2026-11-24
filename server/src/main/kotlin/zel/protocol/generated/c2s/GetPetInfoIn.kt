package zel.protocol.generated

data class GetPetInfoIn(
    val petId: Int,
)

object GetPetInfoInCodec : IncomingCodec<GetPetInfoIn> {
    override val name = "GetPetInfoIn"

    override fun read(r: PacketReader): GetPetInfoIn = readGetPetInfoIn(r)

    override fun write(w: PacketWriter, m: GetPetInfoIn) = writeGetPetInfoIn(w, m)

    private fun readGetPetInfoIn(r: PacketReader): GetPetInfoIn {
        val vPetId = r.int()
        return GetPetInfoIn(vPetId)
    }

    private fun writeGetPetInfoIn(w: PacketWriter, m: GetPetInfoIn) {
        w.int(m.petId)
    }
}
