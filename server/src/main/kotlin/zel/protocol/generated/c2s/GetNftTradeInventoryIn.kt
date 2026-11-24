package zel.protocol.generated

data object GetNftTradeInventoryIn

object GetNftTradeInventoryInCodec : IncomingCodec<GetNftTradeInventoryIn> {
    override val name = "GetNftTradeInventoryIn"

    override fun read(r: PacketReader): GetNftTradeInventoryIn = GetNftTradeInventoryIn

    override fun write(w: PacketWriter, m: GetNftTradeInventoryIn) = writeGetNftTradeInventoryIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetNftTradeInventoryIn(w: PacketWriter, m: GetNftTradeInventoryIn) {
    }
}
