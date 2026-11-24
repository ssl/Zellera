package zel.protocol.generated

data class MessengerErrorOut(
    val int1: Int,
    val int2: Int,
)

object MessengerErrorOutCodec : OutgoingCodec<MessengerErrorOut> {
    override val name = "MessengerErrorOut"

    override fun read(r: PacketReader): MessengerErrorOut = readMessengerErrorOut(r)

    override fun write(w: PacketWriter, m: MessengerErrorOut) = writeMessengerErrorOut(w, m)

    private fun readMessengerErrorOut(r: PacketReader): MessengerErrorOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return MessengerErrorOut(vInt1, vInt2)
    }

    private fun writeMessengerErrorOut(w: PacketWriter, m: MessengerErrorOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
