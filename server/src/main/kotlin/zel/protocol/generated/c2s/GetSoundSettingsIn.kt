package zel.protocol.generated

data object GetSoundSettingsIn

object GetSoundSettingsInCodec : IncomingCodec<GetSoundSettingsIn> {
    override val name = "GetSoundSettingsIn"

    override fun read(r: PacketReader): GetSoundSettingsIn = GetSoundSettingsIn

    override fun write(w: PacketWriter, m: GetSoundSettingsIn) = writeGetSoundSettingsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetSoundSettingsIn(w: PacketWriter, m: GetSoundSettingsIn) {
    }
}
