package zel.protocol.generated

data class DiscordPreferencesOut(
    val int1: Int,
    val boolean1: Boolean,
    val boolean2: Boolean,
    val boolean3: Boolean,
    val boolean4: Boolean,
)

object DiscordPreferencesOutCodec : OutgoingCodec<DiscordPreferencesOut> {
    override val name = "DiscordPreferencesOut"

    override fun read(r: PacketReader): DiscordPreferencesOut = readDiscordPreferencesOut(r)

    override fun write(w: PacketWriter, m: DiscordPreferencesOut) = writeDiscordPreferencesOut(w, m)

    private fun readDiscordPreferencesOut(r: PacketReader): DiscordPreferencesOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vBoolean3 = r.boolean()
        val vBoolean4 = r.boolean()
        return DiscordPreferencesOut(vInt1, vBoolean1, vBoolean2, vBoolean3, vBoolean4)
    }

    private fun writeDiscordPreferencesOut(w: PacketWriter, m: DiscordPreferencesOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.boolean(m.boolean3)
        w.boolean(m.boolean4)
    }
}
