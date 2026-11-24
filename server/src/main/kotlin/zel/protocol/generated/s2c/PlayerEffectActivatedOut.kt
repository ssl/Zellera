package zel.protocol.generated

data class PlayerEffectActivatedOut(
    val effectType: Int,
    val duration: Int,
    val permanent: Boolean,
)

object PlayerEffectActivatedOutCodec : OutgoingCodec<PlayerEffectActivatedOut> {
    override val name = "PlayerEffectActivatedOut"

    override fun read(r: PacketReader): PlayerEffectActivatedOut = readPlayerEffectActivatedOut(r)

    override fun write(w: PacketWriter, m: PlayerEffectActivatedOut) = writePlayerEffectActivatedOut(w, m)

    private fun readPlayerEffectActivatedOut(r: PacketReader): PlayerEffectActivatedOut {
        val vEffectType = r.int()
        val vDuration = r.int()
        val vPermanent = r.boolean()
        return PlayerEffectActivatedOut(vEffectType, vDuration, vPermanent)
    }

    private fun writePlayerEffectActivatedOut(w: PacketWriter, m: PlayerEffectActivatedOut) {
        w.int(m.effectType)
        w.int(m.duration)
        w.boolean(m.permanent)
    }
}
