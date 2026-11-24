package zel.protocol.generated

data class PlayerIconStatusChangedOut(
    val int1: Int,
    val int2: Int,
)

object PlayerIconStatusChangedOutCodec : OutgoingCodec<PlayerIconStatusChangedOut> {
    override val name = "PlayerIconStatusChangedOut"

    override fun read(r: PacketReader): PlayerIconStatusChangedOut = readPlayerIconStatusChangedOut(r)

    override fun write(w: PacketWriter, m: PlayerIconStatusChangedOut) = writePlayerIconStatusChangedOut(w, m)

    private fun readPlayerIconStatusChangedOut(r: PacketReader): PlayerIconStatusChangedOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return PlayerIconStatusChangedOut(vInt1, vInt2)
    }

    private fun writePlayerIconStatusChangedOut(w: PacketWriter, m: PlayerIconStatusChangedOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
