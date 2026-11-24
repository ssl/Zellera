package zel.protocol.generated

data class WiredContractUpdateResultOut(
    val int1: Int,
    val boolean1: Boolean,
    val string1: String,
)

object WiredContractUpdateResultOutCodec : OutgoingCodec<WiredContractUpdateResultOut> {
    override val name = "WiredContractUpdateResultOut"

    override fun read(r: PacketReader): WiredContractUpdateResultOut = readWiredContractUpdateResultOut(r)

    override fun write(w: PacketWriter, m: WiredContractUpdateResultOut) = writeWiredContractUpdateResultOut(w, m)

    private fun readWiredContractUpdateResultOut(r: PacketReader): WiredContractUpdateResultOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        val vString1 = r.string()
        return WiredContractUpdateResultOut(vInt1, vBoolean1, vString1)
    }

    private fun writeWiredContractUpdateResultOut(w: PacketWriter, m: WiredContractUpdateResultOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
        w.string(m.string1)
    }
}
