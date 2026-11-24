package zel.protocol.generated

data class BroadcastOut(
    val string1: String,
)

object BroadcastOutCodec : OutgoingCodec<BroadcastOut> {
    override val name = "BroadcastOut"

    override fun read(r: PacketReader): BroadcastOut = readBroadcastOut(r)

    override fun write(w: PacketWriter, m: BroadcastOut) = writeBroadcastOut(w, m)

    private fun readBroadcastOut(r: PacketReader): BroadcastOut {
        val vString1 = r.string()
        return BroadcastOut(vString1)
    }

    private fun writeBroadcastOut(w: PacketWriter, m: BroadcastOut) {
        w.string(m.string1)
    }
}
