package zel.protocol.generated

data class OpenChestAndGetContentsIn(
    val itemId: Int,
)

object OpenChestAndGetContentsInCodec : IncomingCodec<OpenChestAndGetContentsIn> {
    override val name = "OpenChestAndGetContentsIn"

    override fun read(r: PacketReader): OpenChestAndGetContentsIn = readOpenChestAndGetContentsIn(r)

    override fun write(w: PacketWriter, m: OpenChestAndGetContentsIn) = writeOpenChestAndGetContentsIn(w, m)

    private fun readOpenChestAndGetContentsIn(r: PacketReader): OpenChestAndGetContentsIn {
        val vItemId = r.int()
        return OpenChestAndGetContentsIn(vItemId)
    }

    private fun writeOpenChestAndGetContentsIn(w: PacketWriter, m: OpenChestAndGetContentsIn) {
        w.int(m.itemId)
    }
}
