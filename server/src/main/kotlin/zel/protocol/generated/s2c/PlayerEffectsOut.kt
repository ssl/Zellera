package zel.protocol.generated

data class PlayerEffectsOut(
    val effects: List<EffectsItem>,
) {
    data class EffectsItem(
        val effectType: Int,
        val subType: Int,
        val duration: Int,
        val inactiveCount: Int,
        val secondsLeft: Int,
        val permanent: Boolean,
    )
}

object PlayerEffectsOutCodec : OutgoingCodec<PlayerEffectsOut> {
    override val name = "PlayerEffectsOut"

    override fun read(r: PacketReader): PlayerEffectsOut = readPlayerEffectsOut(r)

    override fun write(w: PacketWriter, m: PlayerEffectsOut) = writePlayerEffectsOut(w, m)

    private fun readPlayerEffectsOutEffectsItem(r: PacketReader): PlayerEffectsOut.EffectsItem {
        val vEffectType = r.int()
        val vSubType = r.int()
        val vDuration = r.int()
        val vInactiveCount = r.int()
        val vSecondsLeft = r.int()
        val vPermanent = r.boolean()
        return PlayerEffectsOut.EffectsItem(vEffectType, vSubType, vDuration, vInactiveCount, vSecondsLeft, vPermanent)
    }

    private fun readPlayerEffectsOut(r: PacketReader): PlayerEffectsOut {
        val vEffects = List(r.countInt(21)) { readPlayerEffectsOutEffectsItem(r) }
        return PlayerEffectsOut(vEffects)
    }

    private fun writePlayerEffectsOutEffectsItem(w: PacketWriter, m: PlayerEffectsOut.EffectsItem) {
        w.int(m.effectType)
        w.int(m.subType)
        w.int(m.duration)
        w.int(m.inactiveCount)
        w.int(m.secondsLeft)
        w.boolean(m.permanent)
    }

    private fun writePlayerEffectsOut(w: PacketWriter, m: PlayerEffectsOut) {
        w.countInt(m.effects.size)
        m.effects.forEach { writePlayerEffectsOutEffectsItem(w, it) }
    }
}
