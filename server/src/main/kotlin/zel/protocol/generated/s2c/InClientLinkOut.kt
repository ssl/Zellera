package zel.protocol.generated

data class InClientLinkOut(
    val string1: String,
)

object InClientLinkOutCodec : OutgoingCodec<InClientLinkOut> {
    override val name = "InClientLinkOut"

    override fun read(r: PacketReader): InClientLinkOut = readInClientLinkOut(r)

    override fun write(w: PacketWriter, m: InClientLinkOut) = writeInClientLinkOut(w, m)

    private fun readInClientLinkOut(r: PacketReader): InClientLinkOut {
        val vString1 = r.string()
        return InClientLinkOut(vString1)
    }

    private fun writeInClientLinkOut(w: PacketWriter, m: InClientLinkOut) {
        w.string(m.string1)
    }
}
