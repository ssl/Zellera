package zel.protocol.generated

data class ScrGetPlayerInfoIn(
    val subscriptionType: String,
)

object ScrGetPlayerInfoInCodec : IncomingCodec<ScrGetPlayerInfoIn> {
    override val name = "ScrGetPlayerInfoIn"

    override fun read(r: PacketReader): ScrGetPlayerInfoIn = readScrGetPlayerInfoIn(r)

    override fun write(w: PacketWriter, m: ScrGetPlayerInfoIn) = writeScrGetPlayerInfoIn(w, m)

    private fun readScrGetPlayerInfoIn(r: PacketReader): ScrGetPlayerInfoIn {
        val vSubscriptionType = r.string()
        return ScrGetPlayerInfoIn(vSubscriptionType)
    }

    private fun writeScrGetPlayerInfoIn(w: PacketWriter, m: ScrGetPlayerInfoIn) {
        w.string(m.subscriptionType)
    }
}
