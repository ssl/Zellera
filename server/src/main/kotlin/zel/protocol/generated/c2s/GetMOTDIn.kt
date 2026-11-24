package zel.protocol.generated

data object GetMOTDIn

object GetMOTDInCodec : IncomingCodec<GetMOTDIn> {
    override val name = "GetMOTDIn"

    override fun read(r: PacketReader): GetMOTDIn = GetMOTDIn

    override fun write(w: PacketWriter, m: GetMOTDIn) = writeGetMOTDIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetMOTDIn(w: PacketWriter, m: GetMOTDIn) {
    }
}
