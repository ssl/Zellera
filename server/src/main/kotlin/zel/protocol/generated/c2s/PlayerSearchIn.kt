package zel.protocol.generated

data class PlayerSearchIn(
    val username: String,
)

object PlayerSearchInCodec : IncomingCodec<PlayerSearchIn> {
    override val name = "PlayerSearchIn"

    override fun read(r: PacketReader): PlayerSearchIn = readPlayerSearchIn(r)

    override fun write(w: PacketWriter, m: PlayerSearchIn) = writePlayerSearchIn(w, m)

    private fun readPlayerSearchIn(r: PacketReader): PlayerSearchIn {
        val vUsername = r.string()
        return PlayerSearchIn(vUsername)
    }

    private fun writePlayerSearchIn(w: PacketWriter, m: PlayerSearchIn) {
        w.string(m.username)
    }
}
