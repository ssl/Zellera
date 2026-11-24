package zel.protocol.generated

data object GetBotInventoryIn

object GetBotInventoryInCodec : IncomingCodec<GetBotInventoryIn> {
    override val name = "GetBotInventoryIn"

    override fun read(r: PacketReader): GetBotInventoryIn = GetBotInventoryIn

    override fun write(w: PacketWriter, m: GetBotInventoryIn) = writeGetBotInventoryIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetBotInventoryIn(w: PacketWriter, m: GetBotInventoryIn) {
    }
}
