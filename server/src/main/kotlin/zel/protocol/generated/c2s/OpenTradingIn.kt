package zel.protocol.generated

data class OpenTradingIn(
    val userId: Int,
)

object OpenTradingInCodec : IncomingCodec<OpenTradingIn> {
    override val name = "OpenTradingIn"

    override fun read(r: PacketReader): OpenTradingIn = readOpenTradingIn(r)

    override fun write(w: PacketWriter, m: OpenTradingIn) = writeOpenTradingIn(w, m)

    private fun readOpenTradingIn(r: PacketReader): OpenTradingIn {
        val vUserId = r.int()
        return OpenTradingIn(vUserId)
    }

    private fun writeOpenTradingIn(w: PacketWriter, m: OpenTradingIn) {
        w.int(m.userId)
    }
}
