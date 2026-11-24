package zel.protocol.generated

data class WiredTransactionSuccessOut(
    val int1: Int,
    val rest: ByteArray,
)

object WiredTransactionSuccessOutCodec : OutgoingCodec<WiredTransactionSuccessOut> {
    override val name = "WiredTransactionSuccessOut"

    override fun read(r: PacketReader): WiredTransactionSuccessOut = readWiredTransactionSuccessOut(r)

    override fun write(w: PacketWriter, m: WiredTransactionSuccessOut) = writeWiredTransactionSuccessOut(w, m)

    private fun readWiredTransactionSuccessOut(r: PacketReader): WiredTransactionSuccessOut {
        val vInt1 = r.int()
        val vRest = r.rest()
        return WiredTransactionSuccessOut(vInt1, vRest)
    }

    private fun writeWiredTransactionSuccessOut(w: PacketWriter, m: WiredTransactionSuccessOut) {
        w.int(m.int1)
        w.bytes(m.rest)
    }
}
