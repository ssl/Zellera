package zel.protocol.generated

data class WiredTransactionGetLogDetailsIn(
    val transactionId: Long,
)

object WiredTransactionGetLogDetailsInCodec : IncomingCodec<WiredTransactionGetLogDetailsIn> {
    override val name = "WiredTransactionGetLogDetailsIn"

    override fun read(r: PacketReader): WiredTransactionGetLogDetailsIn = readWiredTransactionGetLogDetailsIn(r)

    override fun write(w: PacketWriter, m: WiredTransactionGetLogDetailsIn) = writeWiredTransactionGetLogDetailsIn(w, m)

    private fun readWiredTransactionGetLogDetailsIn(r: PacketReader): WiredTransactionGetLogDetailsIn {
        val vTransactionId = r.long()
        return WiredTransactionGetLogDetailsIn(vTransactionId)
    }

    private fun writeWiredTransactionGetLogDetailsIn(w: PacketWriter, m: WiredTransactionGetLogDetailsIn) {
        w.long(m.transactionId)
    }
}
