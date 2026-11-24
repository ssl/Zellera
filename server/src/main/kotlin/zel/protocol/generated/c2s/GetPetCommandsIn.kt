package zel.protocol.generated

data class GetPetCommandsIn(
    val petId: Int,
)

object GetPetCommandsInCodec : IncomingCodec<GetPetCommandsIn> {
    override val name = "GetPetCommandsIn"

    override fun read(r: PacketReader): GetPetCommandsIn = readGetPetCommandsIn(r)

    override fun write(w: PacketWriter, m: GetPetCommandsIn) = writeGetPetCommandsIn(w, m)

    private fun readGetPetCommandsIn(r: PacketReader): GetPetCommandsIn {
        val vPetId = r.int()
        return GetPetCommandsIn(vPetId)
    }

    private fun writeGetPetCommandsIn(w: PacketWriter, m: GetPetCommandsIn) {
        w.int(m.petId)
    }
}
