package zel.protocol.generated

data object GetClientFaqsIn

object GetClientFaqsInCodec : IncomingCodec<GetClientFaqsIn> {
    override val name = "GetClientFaqsIn"

    override fun read(r: PacketReader): GetClientFaqsIn = GetClientFaqsIn

    override fun write(w: PacketWriter, m: GetClientFaqsIn) = writeGetClientFaqsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetClientFaqsIn(w: PacketWriter, m: GetClientFaqsIn) {
    }
}
