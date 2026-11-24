package zel.protocol.generated

data class RespectPetIn(
    val petId: Int,
)

object RespectPetInCodec : IncomingCodec<RespectPetIn> {
    override val name = "RespectPetIn"

    override fun read(r: PacketReader): RespectPetIn = readRespectPetIn(r)

    override fun write(w: PacketWriter, m: RespectPetIn) = writeRespectPetIn(w, m)

    private fun readRespectPetIn(r: PacketReader): RespectPetIn {
        val vPetId = r.int()
        return RespectPetIn(vPetId)
    }

    private fun writeRespectPetIn(w: PacketWriter, m: RespectPetIn) {
        w.int(m.petId)
    }
}
