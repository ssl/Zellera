package zel.protocol.generated

data class PlayerEffectSelectedIn(
    val effectId: Int,
)

object PlayerEffectSelectedInCodec : IncomingCodec<PlayerEffectSelectedIn> {
    override val name = "PlayerEffectSelectedIn"

    override fun read(r: PacketReader): PlayerEffectSelectedIn = readPlayerEffectSelectedIn(r)

    override fun write(w: PacketWriter, m: PlayerEffectSelectedIn) = writePlayerEffectSelectedIn(w, m)

    private fun readPlayerEffectSelectedIn(r: PacketReader): PlayerEffectSelectedIn {
        val vEffectId = r.int()
        return PlayerEffectSelectedIn(vEffectId)
    }

    private fun writePlayerEffectSelectedIn(w: PacketWriter, m: PlayerEffectSelectedIn) {
        w.int(m.effectId)
    }
}
