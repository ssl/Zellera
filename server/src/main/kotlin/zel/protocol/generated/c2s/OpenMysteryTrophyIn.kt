package zel.protocol.generated

data class OpenMysteryTrophyIn(
    val itemId: Int,
    val text: String,
)

object OpenMysteryTrophyInCodec : IncomingCodec<OpenMysteryTrophyIn> {
    override val name = "OpenMysteryTrophyIn"

    override fun read(r: PacketReader): OpenMysteryTrophyIn = readOpenMysteryTrophyIn(r)

    override fun write(w: PacketWriter, m: OpenMysteryTrophyIn) = writeOpenMysteryTrophyIn(w, m)

    private fun readOpenMysteryTrophyIn(r: PacketReader): OpenMysteryTrophyIn {
        val vItemId = r.int()
        val vText = r.string()
        return OpenMysteryTrophyIn(vItemId, vText)
    }

    private fun writeOpenMysteryTrophyIn(w: PacketWriter, m: OpenMysteryTrophyIn) {
        w.int(m.itemId)
        w.string(m.text)
    }
}
