package zel.protocol.generated

data class PlayerEventCatsOut(
    val categories: List<CategoriesItem>,
) {
    data class CategoriesItem(
        val id: Int,
        val caption: String,
        val visible: Boolean,
    )
}

object PlayerEventCatsOutCodec : OutgoingCodec<PlayerEventCatsOut> {
    override val name = "PlayerEventCatsOut"

    override fun read(r: PacketReader): PlayerEventCatsOut = readPlayerEventCatsOut(r)

    override fun write(w: PacketWriter, m: PlayerEventCatsOut) = writePlayerEventCatsOut(w, m)

    private fun readPlayerEventCatsOutCategoriesItem(r: PacketReader): PlayerEventCatsOut.CategoriesItem {
        val vId = r.int()
        val vCaption = r.string()
        val vVisible = r.boolean()
        return PlayerEventCatsOut.CategoriesItem(vId, vCaption, vVisible)
    }

    private fun readPlayerEventCatsOut(r: PacketReader): PlayerEventCatsOut {
        val vCategories = List(r.countInt(7)) { readPlayerEventCatsOutCategoriesItem(r) }
        return PlayerEventCatsOut(vCategories)
    }

    private fun writePlayerEventCatsOutCategoriesItem(w: PacketWriter, m: PlayerEventCatsOut.CategoriesItem) {
        w.int(m.id)
        w.string(m.caption)
        w.boolean(m.visible)
    }

    private fun writePlayerEventCatsOut(w: PacketWriter, m: PlayerEventCatsOut) {
        w.countInt(m.categories.size)
        m.categories.forEach { writePlayerEventCatsOutCategoriesItem(w, it) }
    }
}
