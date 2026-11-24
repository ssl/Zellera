package zel.protocol.generated

data class SpecialRoomEffectOut(
    val int1: Int,
)

object SpecialRoomEffectOutCodec : OutgoingCodec<SpecialRoomEffectOut> {
    override val name = "SpecialRoomEffectOut"

    override fun read(r: PacketReader): SpecialRoomEffectOut = readSpecialRoomEffectOut(r)

    override fun write(w: PacketWriter, m: SpecialRoomEffectOut) = writeSpecialRoomEffectOut(w, m)

    private fun readSpecialRoomEffectOut(r: PacketReader): SpecialRoomEffectOut {
        val vInt1 = r.int()
        return SpecialRoomEffectOut(vInt1)
    }

    private fun writeSpecialRoomEffectOut(w: PacketWriter, m: SpecialRoomEffectOut) {
        w.int(m.int1)
    }
}
