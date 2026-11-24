package zel.protocol.generated

data class PlayerNameChangedOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
)

object PlayerNameChangedOutCodec : OutgoingCodec<PlayerNameChangedOut> {
    override val name = "PlayerNameChangedOut"

    override fun read(r: PacketReader): PlayerNameChangedOut = readPlayerNameChangedOut(r)

    override fun write(w: PacketWriter, m: PlayerNameChangedOut) = writePlayerNameChangedOut(w, m)

    private fun readPlayerNameChangedOut(r: PacketReader): PlayerNameChangedOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        return PlayerNameChangedOut(vInt1, vInt2, vString1)
    }

    private fun writePlayerNameChangedOut(w: PacketWriter, m: PlayerNameChangedOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
    }
}
