package zel.protocol.generated

data class ObjectRemoveMultipleOut(
    val list1: List<Int>,
    val int1: Int,
    val boolean1: Boolean,
)

object ObjectRemoveMultipleOutCodec : OutgoingCodec<ObjectRemoveMultipleOut> {
    override val name = "ObjectRemoveMultipleOut"

    override fun read(r: PacketReader): ObjectRemoveMultipleOut = readObjectRemoveMultipleOut(r)

    override fun write(w: PacketWriter, m: ObjectRemoveMultipleOut) = writeObjectRemoveMultipleOut(w, m)

    private fun readObjectRemoveMultipleOut(r: PacketReader): ObjectRemoveMultipleOut {
        val vList1 = List(r.countInt(4)) { r.int() }
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        return ObjectRemoveMultipleOut(vList1, vInt1, vBoolean1)
    }

    private fun writeObjectRemoveMultipleOut(w: PacketWriter, m: ObjectRemoveMultipleOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.int(m.int1)
        w.boolean(m.boolean1)
    }
}
