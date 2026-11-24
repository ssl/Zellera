package zel.protocol.generated

data class TargetedOfferOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val string2: String,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val int6: Int,
    val int7: Int,
    val string3: String,
    val string4: String,
    val string5: String,
    val string6: String,
    val int8: Int,
    val list1: List<String>,
)

object TargetedOfferOutCodec : OutgoingCodec<TargetedOfferOut> {
    override val name = "TargetedOfferOut"

    override fun read(r: PacketReader): TargetedOfferOut = readTargetedOfferOut(r)

    override fun write(w: PacketWriter, m: TargetedOfferOut) = writeTargetedOfferOut(w, m)

    private fun readTargetedOfferOut(r: PacketReader): TargetedOfferOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vString3 = r.string()
        val vString4 = r.string()
        val vString5 = r.string()
        val vString6 = r.string()
        val vInt8 = r.int()
        val vList1 = List(r.countInt(2)) { r.string() }
        return TargetedOfferOut(vInt1, vInt2, vString1, vString2, vInt3, vInt4, vInt5, vInt6, vInt7, vString3, vString4, vString5, vString6, vInt8, vList1)
    }

    private fun writeTargetedOfferOut(w: PacketWriter, m: TargetedOfferOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.string(m.string3)
        w.string(m.string4)
        w.string(m.string5)
        w.string(m.string6)
        w.int(m.int8)
        w.countInt(m.list1.size)
        m.list1.forEach { w.string(it) }
    }
}
