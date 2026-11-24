package zel.protocol.generated

data class PlayerRightsOut(
    val int1: Int,
    val int2: Int,
    val boolean1: Boolean,
)

object PlayerRightsOutCodec : OutgoingCodec<PlayerRightsOut> {
    override val name = "PlayerRightsOut"

    override fun read(r: PacketReader): PlayerRightsOut = readPlayerRightsOut(r)

    override fun write(w: PacketWriter, m: PlayerRightsOut) = writePlayerRightsOut(w, m)

    private fun readPlayerRightsOut(r: PacketReader): PlayerRightsOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        return PlayerRightsOut(vInt1, vInt2, vBoolean1)
    }

    private fun writePlayerRightsOut(w: PacketWriter, m: PlayerRightsOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.boolean(m.boolean1)
    }
}
