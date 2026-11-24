package zel.protocol.generated

data class SetChestNotificationSettingsIn(
    val itemId: Int,
    val full: Boolean,
    val donation: Boolean,
    val withdraw: Boolean,
    val empty: Boolean,
    val wired: Boolean,
    val mode: Int,
)

object SetChestNotificationSettingsInCodec : IncomingCodec<SetChestNotificationSettingsIn> {
    override val name = "SetChestNotificationSettingsIn"

    override fun read(r: PacketReader): SetChestNotificationSettingsIn = readSetChestNotificationSettingsIn(r)

    override fun write(w: PacketWriter, m: SetChestNotificationSettingsIn) = writeSetChestNotificationSettingsIn(w, m)

    private fun readSetChestNotificationSettingsIn(r: PacketReader): SetChestNotificationSettingsIn {
        val vItemId = r.int()
        val vFull = r.boolean()
        val vDonation = r.boolean()
        val vWithdraw = r.boolean()
        val vEmpty = r.boolean()
        val vWired = r.boolean()
        val vMode = r.int()
        return SetChestNotificationSettingsIn(vItemId, vFull, vDonation, vWithdraw, vEmpty, vWired, vMode)
    }

    private fun writeSetChestNotificationSettingsIn(w: PacketWriter, m: SetChestNotificationSettingsIn) {
        w.int(m.itemId)
        w.boolean(m.full)
        w.boolean(m.donation)
        w.boolean(m.withdraw)
        w.boolean(m.empty)
        w.boolean(m.wired)
        w.int(m.mode)
    }
}
