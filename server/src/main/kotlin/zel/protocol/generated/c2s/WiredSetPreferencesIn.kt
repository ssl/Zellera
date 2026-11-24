package zel.protocol.generated

data class WiredSetPreferencesIn(
    val menuButton: Boolean,
    val inspectButton: Boolean,
    val playtest: Boolean,
    val reserved: Int,
    val wiredWhisperDisabled: Boolean,
    val showAllNotifications: Boolean,
    val uiStyle: String,
)

object WiredSetPreferencesInCodec : IncomingCodec<WiredSetPreferencesIn> {
    override val name = "WiredSetPreferencesIn"

    override fun read(r: PacketReader): WiredSetPreferencesIn = readWiredSetPreferencesIn(r)

    override fun write(w: PacketWriter, m: WiredSetPreferencesIn) = writeWiredSetPreferencesIn(w, m)

    private fun readWiredSetPreferencesIn(r: PacketReader): WiredSetPreferencesIn {
        val vMenuButton = r.boolean()
        val vInspectButton = r.boolean()
        val vPlaytest = r.boolean()
        val vReserved = r.int()
        val vWiredWhisperDisabled = r.boolean()
        val vShowAllNotifications = r.boolean()
        val vUiStyle = r.string()
        return WiredSetPreferencesIn(vMenuButton, vInspectButton, vPlaytest, vReserved, vWiredWhisperDisabled, vShowAllNotifications, vUiStyle)
    }

    private fun writeWiredSetPreferencesIn(w: PacketWriter, m: WiredSetPreferencesIn) {
        w.boolean(m.menuButton)
        w.boolean(m.inspectButton)
        w.boolean(m.playtest)
        w.int(m.reserved)
        w.boolean(m.wiredWhisperDisabled)
        w.boolean(m.showAllNotifications)
        w.string(m.uiStyle)
    }
}
