package zel.protocol.generated

data object GetCreditsInfoIn

object GetCreditsInfoInCodec : IncomingCodec<GetCreditsInfoIn> {
    override val name = "GetCreditsInfoIn"

    override fun read(r: PacketReader): GetCreditsInfoIn = GetCreditsInfoIn

    override fun write(w: PacketWriter, m: GetCreditsInfoIn) = writeGetCreditsInfoIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetCreditsInfoIn(w: PacketWriter, m: GetCreditsInfoIn) {
    }
}
