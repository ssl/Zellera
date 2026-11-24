package zel.protocol.generated

data class PlayerEffectAddedOut(
    val effectType: Int,
    val subType: Int,
    val duration: Int,
    val permanent: Boolean,
)

object PlayerEffectAddedOutCodec : OutgoingCodec<PlayerEffectAddedOut> {
    override val name = "PlayerEffectAddedOut"

    override fun read(r: PacketReader): PlayerEffectAddedOut = readPlayerEffectAddedOut(r)

    override fun write(w: PacketWriter, m: PlayerEffectAddedOut) = writePlayerEffectAddedOut(w, m)

    private fun readPlayerEffectAddedOut(r: PacketReader): PlayerEffectAddedOut {
        val vEffectType = r.int()
        val vSubType = r.int()
        val vDuration = r.int()
        val vPermanent = r.boolean()
        return PlayerEffectAddedOut(vEffectType, vSubType, vDuration, vPermanent)
    }

    private fun writePlayerEffectAddedOut(w: PacketWriter, m: PlayerEffectAddedOut) {
        w.int(m.effectType)
        w.int(m.subType)
        w.int(m.duration)
        w.boolean(m.permanent)
    }
}
