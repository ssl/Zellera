package zel.protocol.generated

data class CloseChestIn(
    val itemId: Int,
)

object CloseChestInCodec : IncomingCodec<CloseChestIn> {
    override val name = "CloseChestIn"

    override fun read(r: PacketReader): CloseChestIn = readCloseChestIn(r)

    override fun write(w: PacketWriter, m: CloseChestIn) = writeCloseChestIn(w, m)

    private fun readCloseChestIn(r: PacketReader): CloseChestIn {
        val vItemId = r.int()
        return CloseChestIn(vItemId)
    }

    private fun writeCloseChestIn(w: PacketWriter, m: CloseChestIn) {
        w.int(m.itemId)
    }
}
