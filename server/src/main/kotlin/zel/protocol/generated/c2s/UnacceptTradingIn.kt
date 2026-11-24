package zel.protocol.generated

data object UnacceptTradingIn

object UnacceptTradingInCodec : IncomingCodec<UnacceptTradingIn> {
    override val name = "UnacceptTradingIn"

    override fun read(r: PacketReader): UnacceptTradingIn = UnacceptTradingIn

    override fun write(w: PacketWriter, m: UnacceptTradingIn) = writeUnacceptTradingIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeUnacceptTradingIn(w: PacketWriter, m: UnacceptTradingIn) {
    }
}
