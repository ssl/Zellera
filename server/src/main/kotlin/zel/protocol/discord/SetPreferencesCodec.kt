package zel.protocol.discord

import zel.protocol.generated.IncomingCodec
import zel.protocol.generated.PacketReader
import zel.protocol.generated.SetDiscordPreferencesIn
import zel.protocol.generated.SetDiscordPreferencesInCodec

internal object SetPreferencesCodec :
    IncomingCodec<SetDiscordPreferencesIn> by SetDiscordPreferencesInCodec {
    override fun read(r: PacketReader) = SetDiscordPreferencesIn(r.int(), r.flag(), r.flag(), r.flag(), r.flag())

    private fun PacketReader.flag() = remaining > 0 && boolean()
}
