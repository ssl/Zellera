package zel.protocol.generated

data class SetDiscordPreferencesIn(
    val version: Int,
    val showPlayer: Boolean,
    val shareActivity: Boolean,
    val hideInHiddenRooms: Boolean,
    val allowJoining: Boolean,
)

object SetDiscordPreferencesInCodec : IncomingCodec<SetDiscordPreferencesIn> {
    override val name = "SetDiscordPreferencesIn"

    override fun read(r: PacketReader): SetDiscordPreferencesIn = readSetDiscordPreferencesIn(r)

    override fun write(w: PacketWriter, m: SetDiscordPreferencesIn) = writeSetDiscordPreferencesIn(w, m)

    private fun readSetDiscordPreferencesIn(r: PacketReader): SetDiscordPreferencesIn {
        val vVersion = r.int()
        val vShowPlayer = r.boolean()
        val vShareActivity = r.boolean()
        val vHideInHiddenRooms = r.boolean()
        val vAllowJoining = r.boolean()
        return SetDiscordPreferencesIn(vVersion, vShowPlayer, vShareActivity, vHideInHiddenRooms, vAllowJoining)
    }

    private fun writeSetDiscordPreferencesIn(w: PacketWriter, m: SetDiscordPreferencesIn) {
        w.int(m.version)
        w.boolean(m.showPlayer)
        w.boolean(m.shareActivity)
        w.boolean(m.hideInHiddenRooms)
        w.boolean(m.allowJoining)
    }
}
