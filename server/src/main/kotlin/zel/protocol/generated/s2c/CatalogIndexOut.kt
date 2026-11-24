package zel.protocol.generated

data class CatalogIndexOut(
    val boolean1: Boolean,
    val int1: Int,
    val int2: Int,
    val string1: String,
    val string2: String,
    val list1: List<Int>,
    val list2: List<List2Item>,
    val boolean2: Boolean,
    val string3: String,
) {
    data class List2Item(
        val node1: Node,
    )
}

data class Node(
    val boolean1: Boolean,
    val int1: Int,
    val int2: Int,
    val string1: String,
    val string2: String,
    val list1: List<Int>,
    val list2: List<List2Item>,
) {
    data class List2Item(
        val node1: Node,
    )
}

object CatalogIndexOutCodec : OutgoingCodec<CatalogIndexOut> {
    override val name = "CatalogIndexOut"

    override fun read(r: PacketReader): CatalogIndexOut = readCatalogIndexOut(r)

    override fun write(w: PacketWriter, m: CatalogIndexOut) = writeCatalogIndexOut(w, m)

    private fun readNodeList2Item(r: PacketReader): Node.List2Item {
        val vNode1 = readNode(r)
        return Node.List2Item(vNode1)
    }

    private fun readNode(r: PacketReader): Node {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vList1 = List(r.countInt(4)) { r.int() }
        val vList2 = List(r.countInt(21)) { readNodeList2Item(r) }
        return Node(vBoolean1, vInt1, vInt2, vString1, vString2, vList1, vList2)
    }

    private fun readCatalogIndexOutList2Item(r: PacketReader): CatalogIndexOut.List2Item {
        val vNode1 = readNode(r)
        return CatalogIndexOut.List2Item(vNode1)
    }

    private fun readCatalogIndexOut(r: PacketReader): CatalogIndexOut {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vList1 = List(r.countInt(4)) { r.int() }
        val vList2 = List(r.countInt(21)) { readCatalogIndexOutList2Item(r) }
        val vBoolean2 = r.boolean()
        val vString3 = r.string()
        return CatalogIndexOut(vBoolean1, vInt1, vInt2, vString1, vString2, vList1, vList2, vBoolean2, vString3)
    }

    private fun writeNodeList2Item(w: PacketWriter, m: Node.List2Item) {
        writeNode(w, m.node1)
    }

    private fun writeNode(w: PacketWriter, m: Node) {
        w.boolean(m.boolean1)
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.string(m.string2)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.countInt(m.list2.size)
        m.list2.forEach { writeNodeList2Item(w, it) }
    }

    private fun writeCatalogIndexOutList2Item(w: PacketWriter, m: CatalogIndexOut.List2Item) {
        writeNode(w, m.node1)
    }

    private fun writeCatalogIndexOut(w: PacketWriter, m: CatalogIndexOut) {
        w.boolean(m.boolean1)
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.string(m.string2)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.countInt(m.list2.size)
        m.list2.forEach { writeCatalogIndexOutList2Item(w, it) }
        w.boolean(m.boolean2)
        w.string(m.string3)
    }
}
