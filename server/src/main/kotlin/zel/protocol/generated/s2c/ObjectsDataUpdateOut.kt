package zel.protocol.generated

data class ObjectsDataUpdateOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val stuff1: StuffData,
    )
}

object ObjectsDataUpdateOutCodec : OutgoingCodec<ObjectsDataUpdateOut> {
    override val name = "ObjectsDataUpdateOut"

    override fun read(r: PacketReader): ObjectsDataUpdateOut = readObjectsDataUpdateOut(r)

    override fun write(w: PacketWriter, m: ObjectsDataUpdateOut) = writeObjectsDataUpdateOut(w, m)

    private fun readObjectsDataUpdateOutList1Item(r: PacketReader): ObjectsDataUpdateOut.List1Item {
        val vInt1 = r.int()
        val vStuff1 = StuffData.read(r)
        return ObjectsDataUpdateOut.List1Item(vInt1, vStuff1)
    }

    private fun readObjectsDataUpdateOut(r: PacketReader): ObjectsDataUpdateOut {
        val vList1 = List(r.countInt(8)) { readObjectsDataUpdateOutList1Item(r) }
        return ObjectsDataUpdateOut(vList1)
    }

    private fun writeObjectsDataUpdateOutList1Item(w: PacketWriter, m: ObjectsDataUpdateOut.List1Item) {
        w.int(m.int1)
        StuffData.write(w, m.stuff1)
    }

    private fun writeObjectsDataUpdateOut(w: PacketWriter, m: ObjectsDataUpdateOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeObjectsDataUpdateOutList1Item(w, it) }
    }
}
