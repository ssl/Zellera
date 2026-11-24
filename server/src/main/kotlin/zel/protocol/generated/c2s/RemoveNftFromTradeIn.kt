package zel.protocol.generated

data class RemoveNftFromTradeIn(
    val int1: Int,
)

object RemoveNftFromTradeInCodec : IncomingCodec<RemoveNftFromTradeIn> {
    override val name = "RemoveNftFromTradeIn"

    override fun read(r: PacketReader): RemoveNftFromTradeIn = readRemoveNftFromTradeIn(r)

    override fun write(w: PacketWriter, m: RemoveNftFromTradeIn) = writeRemoveNftFromTradeIn(w, m)

    private fun readRemoveNftFromTradeIn(r: PacketReader): RemoveNftFromTradeIn {
        val vInt1 = r.int()
        return RemoveNftFromTradeIn(vInt1)
    }

    private fun writeRemoveNftFromTradeIn(w: PacketWriter, m: RemoveNftFromTradeIn) {
        w.int(m.int1)
    }
}
