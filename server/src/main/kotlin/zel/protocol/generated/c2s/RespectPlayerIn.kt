package zel.protocol.generated

data class RespectPlayerIn(
    val userId: Int,
)

object RespectPlayerInCodec : IncomingCodec<RespectPlayerIn> {
    override val name = "RespectPlayerIn"

    override fun read(r: PacketReader): RespectPlayerIn = readRespectPlayerIn(r)

    override fun write(w: PacketWriter, m: RespectPlayerIn) = writeRespectPlayerIn(w, m)

    private fun readRespectPlayerIn(r: PacketReader): RespectPlayerIn {
        val vUserId = r.int()
        return RespectPlayerIn(vUserId)
    }

    private fun writeRespectPlayerIn(w: PacketWriter, m: RespectPlayerIn) {
        w.int(m.userId)
    }
}
