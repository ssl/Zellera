package zel.protocol.generated

data class PlayerBannedOut(
    val string1: String,
)

object PlayerBannedOutCodec : OutgoingCodec<PlayerBannedOut> {
    override val name = "PlayerBannedOut"

    override fun read(r: PacketReader): PlayerBannedOut = readPlayerBannedOut(r)

    override fun write(w: PacketWriter, m: PlayerBannedOut) = writePlayerBannedOut(w, m)

    private fun readPlayerBannedOut(r: PacketReader): PlayerBannedOut {
        val vString1 = r.string()
        return PlayerBannedOut(vString1)
    }

    private fun writePlayerBannedOut(w: PacketWriter, m: PlayerBannedOut) {
        w.string(m.string1)
    }
}
