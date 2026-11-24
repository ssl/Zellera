package zel.protocol.generated

data object GetGiftWrappingConfigurationIn

object GetGiftWrappingConfigurationInCodec : IncomingCodec<GetGiftWrappingConfigurationIn> {
    override val name = "GetGiftWrappingConfigurationIn"

    override fun read(r: PacketReader): GetGiftWrappingConfigurationIn = GetGiftWrappingConfigurationIn

    override fun write(w: PacketWriter, m: GetGiftWrappingConfigurationIn) = writeGetGiftWrappingConfigurationIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetGiftWrappingConfigurationIn(w: PacketWriter, m: GetGiftWrappingConfigurationIn) {
    }
}
