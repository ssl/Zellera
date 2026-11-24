package zel.protocol.generated

data class DoorbellOut(
    val string1: String,
)

object DoorbellOutCodec : OutgoingCodec<DoorbellOut> {
    override val name = "DoorbellOut"

    override fun read(r: PacketReader): DoorbellOut = readDoorbellOut(r)

    override fun write(w: PacketWriter, m: DoorbellOut) = writeDoorbellOut(w, m)

    private fun readDoorbellOut(r: PacketReader): DoorbellOut {
        val vString1 = r.string()
        return DoorbellOut(vString1)
    }

    private fun writeDoorbellOut(w: PacketWriter, m: DoorbellOut) {
        w.string(m.string1)
    }
}
