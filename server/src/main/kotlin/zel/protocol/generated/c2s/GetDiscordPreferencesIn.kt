package zel.protocol.generated

data object GetDiscordPreferencesIn

object GetDiscordPreferencesInCodec : IncomingCodec<GetDiscordPreferencesIn> {
    override val name = "GetDiscordPreferencesIn"

    override fun read(r: PacketReader): GetDiscordPreferencesIn = GetDiscordPreferencesIn

    override fun write(w: PacketWriter, m: GetDiscordPreferencesIn) = writeGetDiscordPreferencesIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetDiscordPreferencesIn(w: PacketWriter, m: GetDiscordPreferencesIn) {
    }
}
