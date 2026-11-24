package zel.protocol.generated

data class PlayerTypingOut(
    val int1: Int,
    val int2: Int,
)

object PlayerTypingOutCodec : OutgoingCodec<PlayerTypingOut> {
    override val name = "PlayerTypingOut"

    override fun read(r: PacketReader): PlayerTypingOut = readPlayerTypingOut(r)

    override fun write(w: PacketWriter, m: PlayerTypingOut) = writePlayerTypingOut(w, m)

    private fun readPlayerTypingOut(r: PacketReader): PlayerTypingOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return PlayerTypingOut(vInt1, vInt2)
    }

    private fun writePlayerTypingOut(w: PacketWriter, m: PlayerTypingOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
