package zel.protocol.generated

data class WiredGenerateWebApiKeyIn(
    val itemId: Int,
    val isReadKey: Boolean,
)

object WiredGenerateWebApiKeyInCodec : IncomingCodec<WiredGenerateWebApiKeyIn> {
    override val name = "WiredGenerateWebApiKeyIn"

    override fun read(r: PacketReader): WiredGenerateWebApiKeyIn = readWiredGenerateWebApiKeyIn(r)

    override fun write(w: PacketWriter, m: WiredGenerateWebApiKeyIn) = writeWiredGenerateWebApiKeyIn(w, m)

    private fun readWiredGenerateWebApiKeyIn(r: PacketReader): WiredGenerateWebApiKeyIn {
        val vItemId = r.int()
        val vIsReadKey = r.boolean()
        return WiredGenerateWebApiKeyIn(vItemId, vIsReadKey)
    }

    private fun writeWiredGenerateWebApiKeyIn(w: PacketWriter, m: WiredGenerateWebApiKeyIn) {
        w.int(m.itemId)
        w.boolean(m.isReadKey)
    }
}
