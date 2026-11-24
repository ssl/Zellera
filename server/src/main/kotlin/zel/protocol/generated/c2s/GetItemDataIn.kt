package zel.protocol.generated

data class GetItemDataIn(
    val itemId: Int,
)

object GetItemDataInCodec : IncomingCodec<GetItemDataIn> {
    override val name = "GetItemDataIn"

    override fun read(r: PacketReader): GetItemDataIn = readGetItemDataIn(r)

    override fun write(w: PacketWriter, m: GetItemDataIn) = writeGetItemDataIn(w, m)

    private fun readGetItemDataIn(r: PacketReader): GetItemDataIn {
        val vItemId = r.int()
        return GetItemDataIn(vItemId)
    }

    private fun writeGetItemDataIn(w: PacketWriter, m: GetItemDataIn) {
        w.int(m.itemId)
    }
}
