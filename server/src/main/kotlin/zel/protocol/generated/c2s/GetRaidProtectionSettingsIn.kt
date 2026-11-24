package zel.protocol.generated

data class GetRaidProtectionSettingsIn(
    val roomId: Int,
)

object GetRaidProtectionSettingsInCodec : IncomingCodec<GetRaidProtectionSettingsIn> {
    override val name = "GetRaidProtectionSettingsIn"

    override fun read(r: PacketReader): GetRaidProtectionSettingsIn = readGetRaidProtectionSettingsIn(r)

    override fun write(w: PacketWriter, m: GetRaidProtectionSettingsIn) = writeGetRaidProtectionSettingsIn(w, m)

    private fun readGetRaidProtectionSettingsIn(r: PacketReader): GetRaidProtectionSettingsIn {
        val vRoomId = r.int()
        return GetRaidProtectionSettingsIn(vRoomId)
    }

    private fun writeGetRaidProtectionSettingsIn(w: PacketWriter, m: GetRaidProtectionSettingsIn) {
        w.int(m.roomId)
    }
}
