package zel.protocol.generated

data class PlayerEffectOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
)

object PlayerEffectOutCodec : OutgoingCodec<PlayerEffectOut> {
    override val name = "PlayerEffectOut"

    override fun read(r: PacketReader): PlayerEffectOut = readPlayerEffectOut(r)

    override fun write(w: PacketWriter, m: PlayerEffectOut) = writePlayerEffectOut(w, m)

    private fun readPlayerEffectOut(r: PacketReader): PlayerEffectOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return PlayerEffectOut(vInt1, vInt2, vInt3)
    }

    private fun writePlayerEffectOut(w: PacketWriter, m: PlayerEffectOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
