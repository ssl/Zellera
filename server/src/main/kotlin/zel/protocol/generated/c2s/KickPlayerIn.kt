package zel.protocol.generated

data class KickPlayerIn(
    val userId: Int,
)

object KickPlayerInCodec : IncomingCodec<KickPlayerIn> {
    override val name = "KickPlayerIn"

    override fun read(r: PacketReader): KickPlayerIn = readKickPlayerIn(r)

    override fun write(w: PacketWriter, m: KickPlayerIn) = writeKickPlayerIn(w, m)

    private fun readKickPlayerIn(r: PacketReader): KickPlayerIn {
        val vUserId = r.int()
        return KickPlayerIn(vUserId)
    }

    private fun writeKickPlayerIn(w: PacketWriter, m: KickPlayerIn) {
        w.int(m.userId)
    }
}
