package zel.protocol.generated

data class PlayerEffectSelectedOut(
    val effectType: Int,
)

object PlayerEffectSelectedOutCodec : OutgoingCodec<PlayerEffectSelectedOut> {
    override val name = "PlayerEffectSelectedOut"

    override fun read(r: PacketReader): PlayerEffectSelectedOut = readPlayerEffectSelectedOut(r)

    override fun write(w: PacketWriter, m: PlayerEffectSelectedOut) = writePlayerEffectSelectedOut(w, m)

    private fun readPlayerEffectSelectedOut(r: PacketReader): PlayerEffectSelectedOut {
        val vEffectType = r.int()
        return PlayerEffectSelectedOut(vEffectType)
    }

    private fun writePlayerEffectSelectedOut(w: PacketWriter, m: PlayerEffectSelectedOut) {
        w.int(m.effectType)
    }
}
