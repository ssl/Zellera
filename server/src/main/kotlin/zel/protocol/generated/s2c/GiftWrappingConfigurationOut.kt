package zel.protocol.generated

data class GiftWrappingConfigurationOut(
    val boolean1: Boolean,
    val int1: Int,
    val list1: List<Int>,
    val list2: List<Int>,
    val list3: List<Int>,
    val list4: List<Int>,
)

object GiftWrappingConfigurationOutCodec : OutgoingCodec<GiftWrappingConfigurationOut> {
    override val name = "GiftWrappingConfigurationOut"

    override fun read(r: PacketReader): GiftWrappingConfigurationOut = readGiftWrappingConfigurationOut(r)

    override fun write(w: PacketWriter, m: GiftWrappingConfigurationOut) = writeGiftWrappingConfigurationOut(w, m)

    private fun readGiftWrappingConfigurationOut(r: PacketReader): GiftWrappingConfigurationOut {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vList1 = List(r.countInt(4)) { r.int() }
        val vList2 = List(r.countInt(4)) { r.int() }
        val vList3 = List(r.countInt(4)) { r.int() }
        val vList4 = List(r.countInt(4)) { r.int() }
        return GiftWrappingConfigurationOut(vBoolean1, vInt1, vList1, vList2, vList3, vList4)
    }

    private fun writeGiftWrappingConfigurationOut(w: PacketWriter, m: GiftWrappingConfigurationOut) {
        w.boolean(m.boolean1)
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.countInt(m.list2.size)
        m.list2.forEach { w.int(it) }
        w.countInt(m.list3.size)
        m.list3.forEach { w.int(it) }
        w.countInt(m.list4.size)
        m.list4.forEach { w.int(it) }
    }
}
