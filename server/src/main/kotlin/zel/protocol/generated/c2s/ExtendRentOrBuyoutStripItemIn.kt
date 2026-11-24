package zel.protocol.generated

data class ExtendRentOrBuyoutStripItemIn(
    val stripId: Int,
    val buyout: Boolean,
)

object ExtendRentOrBuyoutStripItemInCodec : IncomingCodec<ExtendRentOrBuyoutStripItemIn> {
    override val name = "ExtendRentOrBuyoutStripItemIn"

    override fun read(r: PacketReader): ExtendRentOrBuyoutStripItemIn = readExtendRentOrBuyoutStripItemIn(r)

    override fun write(w: PacketWriter, m: ExtendRentOrBuyoutStripItemIn) = writeExtendRentOrBuyoutStripItemIn(w, m)

    private fun readExtendRentOrBuyoutStripItemIn(r: PacketReader): ExtendRentOrBuyoutStripItemIn {
        val vStripId = r.int()
        val vBuyout = r.boolean()
        return ExtendRentOrBuyoutStripItemIn(vStripId, vBuyout)
    }

    private fun writeExtendRentOrBuyoutStripItemIn(w: PacketWriter, m: ExtendRentOrBuyoutStripItemIn) {
        w.int(m.stripId)
        w.boolean(m.buyout)
    }
}
