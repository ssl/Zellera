package zel.protocol.generated

data class PlayerEffectActivatedIn(
    val effectId: Int,
)

object PlayerEffectActivatedInCodec : IncomingCodec<PlayerEffectActivatedIn> {
    override val name = "PlayerEffectActivatedIn"

    override fun read(r: PacketReader): PlayerEffectActivatedIn = readPlayerEffectActivatedIn(r)

    override fun write(w: PacketWriter, m: PlayerEffectActivatedIn) = writePlayerEffectActivatedIn(w, m)

    private fun readPlayerEffectActivatedIn(r: PacketReader): PlayerEffectActivatedIn {
        val vEffectId = r.int()
        return PlayerEffectActivatedIn(vEffectId)
    }

    private fun writePlayerEffectActivatedIn(w: PacketWriter, m: PlayerEffectActivatedIn) {
        w.int(m.effectId)
    }
}
