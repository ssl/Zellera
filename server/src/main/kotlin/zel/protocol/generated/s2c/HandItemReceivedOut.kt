package zel.protocol.generated

data class HandItemReceivedOut(
    val int1: Int,
    val int2: Int,
)

object HandItemReceivedOutCodec : OutgoingCodec<HandItemReceivedOut> {
    override val name = "HandItemReceivedOut"

    override fun read(r: PacketReader): HandItemReceivedOut = readHandItemReceivedOut(r)

    override fun write(w: PacketWriter, m: HandItemReceivedOut) = writeHandItemReceivedOut(w, m)

    private fun readHandItemReceivedOut(r: PacketReader): HandItemReceivedOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return HandItemReceivedOut(vInt1, vInt2)
    }

    private fun writeHandItemReceivedOut(w: PacketWriter, m: HandItemReceivedOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
