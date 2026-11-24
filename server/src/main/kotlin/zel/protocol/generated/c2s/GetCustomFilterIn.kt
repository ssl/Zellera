package zel.protocol.generated

data object GetCustomFilterIn

object GetCustomFilterInCodec : IncomingCodec<GetCustomFilterIn> {
    override val name = "GetCustomFilterIn"

    override fun read(r: PacketReader): GetCustomFilterIn = GetCustomFilterIn

    override fun write(w: PacketWriter, m: GetCustomFilterIn) = writeGetCustomFilterIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetCustomFilterIn(w: PacketWriter, m: GetCustomFilterIn) {
    }
}
