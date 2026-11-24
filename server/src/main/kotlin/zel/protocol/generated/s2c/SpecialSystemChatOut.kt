package zel.protocol.generated

data class SpecialSystemChatOut(
    val int1: Int,
    val int2: Int,
)

object SpecialSystemChatOutCodec : OutgoingCodec<SpecialSystemChatOut> {
    override val name = "SpecialSystemChatOut"

    override fun read(r: PacketReader): SpecialSystemChatOut = readSpecialSystemChatOut(r)

    override fun write(w: PacketWriter, m: SpecialSystemChatOut) = writeSpecialSystemChatOut(w, m)

    private fun readSpecialSystemChatOut(r: PacketReader): SpecialSystemChatOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return SpecialSystemChatOut(vInt1, vInt2)
    }

    private fun writeSpecialSystemChatOut(w: PacketWriter, m: SpecialSystemChatOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
