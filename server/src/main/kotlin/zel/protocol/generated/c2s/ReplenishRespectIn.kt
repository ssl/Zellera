package zel.protocol.generated

data object ReplenishRespectIn

object ReplenishRespectInCodec : IncomingCodec<ReplenishRespectIn> {
    override val name = "ReplenishRespectIn"

    override fun read(r: PacketReader): ReplenishRespectIn = ReplenishRespectIn

    override fun write(w: PacketWriter, m: ReplenishRespectIn) = writeReplenishRespectIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeReplenishRespectIn(w: PacketWriter, m: ReplenishRespectIn) {
    }
}
