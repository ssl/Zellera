package zel.protocol.generated

data class ItemRemoveMultipleOut(
    val list1: List<Int>,
    val int1: Int,
    val boolean1: Boolean,
)

object ItemRemoveMultipleOutCodec : OutgoingCodec<ItemRemoveMultipleOut> {
    override val name = "ItemRemoveMultipleOut"

    override fun read(r: PacketReader): ItemRemoveMultipleOut = readItemRemoveMultipleOut(r)

    override fun write(w: PacketWriter, m: ItemRemoveMultipleOut) = writeItemRemoveMultipleOut(w, m)

    private fun readItemRemoveMultipleOut(r: PacketReader): ItemRemoveMultipleOut {
        val vList1 = List(r.countInt(4)) { r.int() }
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        return ItemRemoveMultipleOut(vList1, vInt1, vBoolean1)
    }

    private fun writeItemRemoveMultipleOut(w: PacketWriter, m: ItemRemoveMultipleOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.int(m.int1)
        w.boolean(m.boolean1)
    }
}
