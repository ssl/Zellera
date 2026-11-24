package zel.protocol.generated

data class SetChestPreferencesIn(
    val itemId: Int,
    val name: String,
    val description: String,
    val accessOpen: Boolean,
    val accessDonate: Boolean,
    val appearanceState: Int,
    val previewMode: Int,
    val previewAmount: Int,
    val enableWired: Boolean,
)

object SetChestPreferencesInCodec : IncomingCodec<SetChestPreferencesIn> {
    override val name = "SetChestPreferencesIn"

    override fun read(r: PacketReader): SetChestPreferencesIn = readSetChestPreferencesIn(r)

    override fun write(w: PacketWriter, m: SetChestPreferencesIn) = writeSetChestPreferencesIn(w, m)

    private fun readSetChestPreferencesIn(r: PacketReader): SetChestPreferencesIn {
        val vItemId = r.int()
        val vName = r.string()
        val vDescription = r.string()
        val vAccessOpen = r.boolean()
        val vAccessDonate = r.boolean()
        val vAppearanceState = r.int()
        val vPreviewMode = r.int()
        val vPreviewAmount = r.int()
        val vEnableWired = r.boolean()
        return SetChestPreferencesIn(vItemId, vName, vDescription, vAccessOpen, vAccessDonate, vAppearanceState, vPreviewMode, vPreviewAmount, vEnableWired)
    }

    private fun writeSetChestPreferencesIn(w: PacketWriter, m: SetChestPreferencesIn) {
        w.int(m.itemId)
        w.string(m.name)
        w.string(m.description)
        w.boolean(m.accessOpen)
        w.boolean(m.accessDonate)
        w.int(m.appearanceState)
        w.int(m.previewMode)
        w.int(m.previewAmount)
        w.boolean(m.enableWired)
    }
}
