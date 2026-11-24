package zel.protocol.generated

data object GetNftCreditsIn

object GetNftCreditsInCodec : IncomingCodec<GetNftCreditsIn> {
    override val name = "GetNftCreditsIn"

    override fun read(r: PacketReader): GetNftCreditsIn = GetNftCreditsIn

    override fun write(w: PacketWriter, m: GetNftCreditsIn) = writeGetNftCreditsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetNftCreditsIn(w: PacketWriter, m: GetNftCreditsIn) {
    }
}
