package zel.protocol.generated

data class PlayerEffectExpiredOut(
    val effectType: Int,
)

object PlayerEffectExpiredOutCodec : OutgoingCodec<PlayerEffectExpiredOut> {
    override val name = "PlayerEffectExpiredOut"

    override fun read(r: PacketReader): PlayerEffectExpiredOut = readPlayerEffectExpiredOut(r)

    override fun write(w: PacketWriter, m: PlayerEffectExpiredOut) = writePlayerEffectExpiredOut(w, m)

    private fun readPlayerEffectExpiredOut(r: PacketReader): PlayerEffectExpiredOut {
        val vEffectType = r.int()
        return PlayerEffectExpiredOut(vEffectType)
    }

    private fun writePlayerEffectExpiredOut(w: PacketWriter, m: PlayerEffectExpiredOut) {
        w.int(m.effectType)
    }
}
