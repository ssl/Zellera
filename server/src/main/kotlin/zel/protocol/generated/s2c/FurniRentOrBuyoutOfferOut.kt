package zel.protocol.generated

data class FurniRentOrBuyoutOfferOut(
    val boolean1: Boolean,
    val string1: String,
    val boolean2: Boolean,
    val int1: Int,
    val int2: Int,
    val int3: Int,
)

object FurniRentOrBuyoutOfferOutCodec : OutgoingCodec<FurniRentOrBuyoutOfferOut> {
    override val name = "FurniRentOrBuyoutOfferOut"

    override fun read(r: PacketReader): FurniRentOrBuyoutOfferOut = readFurniRentOrBuyoutOfferOut(r)

    override fun write(w: PacketWriter, m: FurniRentOrBuyoutOfferOut) = writeFurniRentOrBuyoutOfferOut(w, m)

    private fun readFurniRentOrBuyoutOfferOut(r: PacketReader): FurniRentOrBuyoutOfferOut {
        val vBoolean1 = r.boolean()
        val vString1 = r.string()
        val vBoolean2 = r.boolean()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return FurniRentOrBuyoutOfferOut(vBoolean1, vString1, vBoolean2, vInt1, vInt2, vInt3)
    }

    private fun writeFurniRentOrBuyoutOfferOut(w: PacketWriter, m: FurniRentOrBuyoutOfferOut) {
        w.boolean(m.boolean1)
        w.string(m.string1)
        w.boolean(m.boolean2)
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
