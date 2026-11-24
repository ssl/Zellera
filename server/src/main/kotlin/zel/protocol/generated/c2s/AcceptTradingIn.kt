package zel.protocol.generated

data object AcceptTradingIn

object AcceptTradingInCodec : IncomingCodec<AcceptTradingIn> {
    override val name = "AcceptTradingIn"

    override fun read(r: PacketReader): AcceptTradingIn = AcceptTradingIn

    override fun write(w: PacketWriter, m: AcceptTradingIn) = writeAcceptTradingIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeAcceptTradingIn(w: PacketWriter, m: AcceptTradingIn) {
    }
}
