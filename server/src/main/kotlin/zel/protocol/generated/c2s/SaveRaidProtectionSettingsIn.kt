package zel.protocol.generated

data class SaveRaidProtectionSettingsIn(
    val roomId: Int,
    val wanted: Boolean,
    val wanted2: Int,
    val wanted3: Int,
    val wanted4: Int,
    val wanted5: Boolean,
    val wanted6: Int,
    val wanted7: Int,
    val confirmed: Boolean,
)

object SaveRaidProtectionSettingsInCodec : IncomingCodec<SaveRaidProtectionSettingsIn> {
    override val name = "SaveRaidProtectionSettingsIn"

    override fun read(r: PacketReader): SaveRaidProtectionSettingsIn = readSaveRaidProtectionSettingsIn(r)

    override fun write(w: PacketWriter, m: SaveRaidProtectionSettingsIn) = writeSaveRaidProtectionSettingsIn(w, m)

    private fun readSaveRaidProtectionSettingsIn(r: PacketReader): SaveRaidProtectionSettingsIn {
        val vRoomId = r.int()
        val vWanted = r.boolean()
        val vWanted2 = r.int()
        val vWanted3 = r.int()
        val vWanted4 = r.int()
        val vWanted5 = r.boolean()
        val vWanted6 = r.int()
        val vWanted7 = r.int()
        val vConfirmed = r.boolean()
        return SaveRaidProtectionSettingsIn(vRoomId, vWanted, vWanted2, vWanted3, vWanted4, vWanted5, vWanted6, vWanted7, vConfirmed)
    }

    private fun writeSaveRaidProtectionSettingsIn(w: PacketWriter, m: SaveRaidProtectionSettingsIn) {
        w.int(m.roomId)
        w.boolean(m.wanted)
        w.int(m.wanted2)
        w.int(m.wanted3)
        w.int(m.wanted4)
        w.boolean(m.wanted5)
        w.int(m.wanted6)
        w.int(m.wanted7)
        w.boolean(m.confirmed)
    }
}
