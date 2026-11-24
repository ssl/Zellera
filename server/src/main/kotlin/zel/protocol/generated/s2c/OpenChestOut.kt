package zel.protocol.generated

data class OpenChestOut(
    val int1: Int,
)

object OpenChestOutCodec : OutgoingCodec<OpenChestOut> {
    override val name = "OpenChestOut"

    override fun read(r: PacketReader): OpenChestOut = readOpenChestOut(r)

    override fun write(w: PacketWriter, m: OpenChestOut) = writeOpenChestOut(w, m)

    private fun readOpenChestOut(r: PacketReader): OpenChestOut {
        val vInt1 = r.int()
        return OpenChestOut(vInt1)
    }

    private fun writeOpenChestOut(w: PacketWriter, m: OpenChestOut) {
        w.int(m.int1)
    }
}
