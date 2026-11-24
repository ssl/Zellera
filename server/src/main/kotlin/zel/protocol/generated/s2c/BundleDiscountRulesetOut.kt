package zel.protocol.generated

data class BundleDiscountRulesetOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val list1: List<Int>,
)

object BundleDiscountRulesetOutCodec : OutgoingCodec<BundleDiscountRulesetOut> {
    override val name = "BundleDiscountRulesetOut"

    override fun read(r: PacketReader): BundleDiscountRulesetOut = readBundleDiscountRulesetOut(r)

    override fun write(w: PacketWriter, m: BundleDiscountRulesetOut) = writeBundleDiscountRulesetOut(w, m)

    private fun readBundleDiscountRulesetOut(r: PacketReader): BundleDiscountRulesetOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vList1 = List(r.countInt(4)) { r.int() }
        return BundleDiscountRulesetOut(vInt1, vInt2, vInt3, vInt4, vList1)
    }

    private fun writeBundleDiscountRulesetOut(w: PacketWriter, m: BundleDiscountRulesetOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
    }
}
