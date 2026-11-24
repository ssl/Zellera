package zel.protocol.generated

data class RedeemNftLootBoxStateOut(
    val short1: Short,
    val int1: Int,
    val short2: Short,
    val string1: String,
    val int2: Int,
    val string2: String,
    val list1: List<Int>,
    val string3: String,
    val string4: String,
)

object RedeemNftLootBoxStateOutCodec : OutgoingCodec<RedeemNftLootBoxStateOut> {
    override val name = "RedeemNftLootBoxStateOut"

    override fun read(r: PacketReader): RedeemNftLootBoxStateOut = readRedeemNftLootBoxStateOut(r)

    override fun write(w: PacketWriter, m: RedeemNftLootBoxStateOut) = writeRedeemNftLootBoxStateOut(w, m)

    private fun readRedeemNftLootBoxStateOut(r: PacketReader): RedeemNftLootBoxStateOut {
        val vShort1 = r.short()
        val vInt1 = r.int()
        val vShort2 = r.short()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vString2 = r.string()
        val vList1 = List(r.countInt(4)) { r.int() }
        val vString3 = r.string()
        val vString4 = r.string()
        return RedeemNftLootBoxStateOut(vShort1, vInt1, vShort2, vString1, vInt2, vString2, vList1, vString3, vString4)
    }

    private fun writeRedeemNftLootBoxStateOut(w: PacketWriter, m: RedeemNftLootBoxStateOut) {
        w.short(m.short1)
        w.int(m.int1)
        w.short(m.short2)
        w.string(m.string1)
        w.int(m.int2)
        w.string(m.string2)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.string(m.string3)
        w.string(m.string4)
    }
}
