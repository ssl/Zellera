package zel.protocol.generated

data class WiredTransactionFailOut(
    val int1: Int,
)

object WiredTransactionFailOutCodec : OutgoingCodec<WiredTransactionFailOut> {
    override val name = "WiredTransactionFailOut"

    override fun read(r: PacketReader): WiredTransactionFailOut = readWiredTransactionFailOut(r)

    override fun write(w: PacketWriter, m: WiredTransactionFailOut) = writeWiredTransactionFailOut(w, m)

    private fun readWiredTransactionFailOut(r: PacketReader): WiredTransactionFailOut {
        val vInt1 = r.int()
        return WiredTransactionFailOut(vInt1)
    }

    private fun writeWiredTransactionFailOut(w: PacketWriter, m: WiredTransactionFailOut) {
        w.int(m.int1)
    }
}
