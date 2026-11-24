package zel.protocol.generated

data class AddSpamWallPostItIn(
    val itemId: Int,
    val string1: String,
    val color: String,
    val text: String,
)

object AddSpamWallPostItInCodec : IncomingCodec<AddSpamWallPostItIn> {
    override val name = "AddSpamWallPostItIn"

    override fun read(r: PacketReader): AddSpamWallPostItIn = readAddSpamWallPostItIn(r)

    override fun write(w: PacketWriter, m: AddSpamWallPostItIn) = writeAddSpamWallPostItIn(w, m)

    private fun readAddSpamWallPostItIn(r: PacketReader): AddSpamWallPostItIn {
        val vItemId = r.int()
        val vString1 = r.string()
        val vColor = r.string()
        val vText = r.string()
        return AddSpamWallPostItIn(vItemId, vString1, vColor, vText)
    }

    private fun writeAddSpamWallPostItIn(w: PacketWriter, m: AddSpamWallPostItIn) {
        w.int(m.itemId)
        w.string(m.string1)
        w.string(m.color)
        w.string(m.text)
    }
}
