package zel.protocol.generated

data class PlayerFlatCatsOut(
    val categories: List<CategoriesItem>,
) {
    data class CategoriesItem(
        val id: Int,
        val name: String,
        val visible: Boolean,
        val automatic: Boolean,
        val automaticCategoryKey: String,
        val globalCategoryKey: String,
        val staffOnly: Boolean,
    )
}

object PlayerFlatCatsOutCodec : OutgoingCodec<PlayerFlatCatsOut> {
    override val name = "PlayerFlatCatsOut"

    override fun read(r: PacketReader): PlayerFlatCatsOut = readPlayerFlatCatsOut(r)

    override fun write(w: PacketWriter, m: PlayerFlatCatsOut) = writePlayerFlatCatsOut(w, m)

    private fun readPlayerFlatCatsOutCategoriesItem(r: PacketReader): PlayerFlatCatsOut.CategoriesItem {
        val vId = r.int()
        val vName = r.string()
        val vVisible = r.boolean()
        val vAutomatic = r.boolean()
        val vAutomaticCategoryKey = r.string()
        val vGlobalCategoryKey = r.string()
        val vStaffOnly = r.boolean()
        return PlayerFlatCatsOut.CategoriesItem(vId, vName, vVisible, vAutomatic, vAutomaticCategoryKey, vGlobalCategoryKey, vStaffOnly)
    }

    private fun readPlayerFlatCatsOut(r: PacketReader): PlayerFlatCatsOut {
        val vCategories = List(r.countInt(13)) { readPlayerFlatCatsOutCategoriesItem(r) }
        return PlayerFlatCatsOut(vCategories)
    }

    private fun writePlayerFlatCatsOutCategoriesItem(w: PacketWriter, m: PlayerFlatCatsOut.CategoriesItem) {
        w.int(m.id)
        w.string(m.name)
        w.boolean(m.visible)
        w.boolean(m.automatic)
        w.string(m.automaticCategoryKey)
        w.string(m.globalCategoryKey)
        w.boolean(m.staffOnly)
    }

    private fun writePlayerFlatCatsOut(w: PacketWriter, m: PlayerFlatCatsOut) {
        w.countInt(m.categories.size)
        m.categories.forEach { writePlayerFlatCatsOutCategoriesItem(w, it) }
    }
}
