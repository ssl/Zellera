package zel.protocol.generated

data class BanPlayerWithDurationIn(
    val userId: Int,
    val roomId: Int,
    val banName: String,
)

object BanPlayerWithDurationInCodec : IncomingCodec<BanPlayerWithDurationIn> {
    override val name = "BanPlayerWithDurationIn"

    override fun read(r: PacketReader): BanPlayerWithDurationIn = readBanPlayerWithDurationIn(r)

    override fun write(w: PacketWriter, m: BanPlayerWithDurationIn) = writeBanPlayerWithDurationIn(w, m)

    private fun readBanPlayerWithDurationIn(r: PacketReader): BanPlayerWithDurationIn {
        val vUserId = r.int()
        val vRoomId = r.int()
        val vBanName = r.string()
        return BanPlayerWithDurationIn(vUserId, vRoomId, vBanName)
    }

    private fun writeBanPlayerWithDurationIn(w: PacketWriter, m: BanPlayerWithDurationIn) {
        w.int(m.userId)
        w.int(m.roomId)
        w.string(m.banName)
    }
}
