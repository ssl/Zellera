package zel.protocol.generated

data class CatalogPageOut(
    val int1: Int,
    val string1: String,
    val string2: String,
    val list1: List<String>,
    val list2: List<String>,
    val list3: List<List3Item>,
    val int2: Int,
    val boolean1: Boolean,
    val rest: ByteArray,
) {
    data class List3Item(
        val int1: Int,
        val string1: String,
        val boolean1: Boolean,
        val int2: Int,
        val int3: Int,
        val int4: Int,
        val int5: Int,
        val boolean2: Boolean,
        val list1: List<List1Item>,
        val int6: Int,
        val boolean3: Boolean,
        val boolean4: Boolean,
        val string2: String,
    ) {
        data class List1Item(
            val string1: String,
            val branch1: Branch1,
        ) {
            sealed interface Branch1 {
                data class Then(
                    val string1: String,
                ) : Branch1
                data class Else(
                    val int1: Int,
                    val string1: String,
                    val int2: Int,
                    val boolean1: Boolean,
                    val branch1: Branch1,
                ) : Branch1 {
                    sealed interface Branch1 {
                        data class Then(
                            val int1: Int,
                            val int2: Int,
                        ) : Branch1
                        data object Else : Branch1
                    }
                }
            }
        }
    }
}

object CatalogPageOutCodec : OutgoingCodec<CatalogPageOut> {
    override val name = "CatalogPageOut"

    override fun read(r: PacketReader): CatalogPageOut = readCatalogPageOut(r)

    override fun write(w: PacketWriter, m: CatalogPageOut) = writeCatalogPageOut(w, m)

    private fun readCatalogPageOutList3ItemList1ItemBranch1Then(r: PacketReader): CatalogPageOut.List3Item.List1Item.Branch1.Then {
        val vString1 = r.string()
        return CatalogPageOut.List3Item.List1Item.Branch1.Then(vString1)
    }

    private fun readCatalogPageOutList3ItemList1ItemBranch1ElseBranch1Then(r: PacketReader): CatalogPageOut.List3Item.List1Item.Branch1.Else.Branch1.Then {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return CatalogPageOut.List3Item.List1Item.Branch1.Else.Branch1.Then(vInt1, vInt2)
    }

    private fun readCatalogPageOutList3ItemList1ItemBranch1Else(r: PacketReader): CatalogPageOut.List3Item.List1Item.Branch1.Else {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        val vBranch1 = if (vBoolean1 == true) readCatalogPageOutList3ItemList1ItemBranch1ElseBranch1Then(r) else CatalogPageOut.List3Item.List1Item.Branch1.Else.Branch1.Else
        return CatalogPageOut.List3Item.List1Item.Branch1.Else(vInt1, vString1, vInt2, vBoolean1, vBranch1)
    }

    private fun readCatalogPageOutList3ItemList1Item(r: PacketReader): CatalogPageOut.List3Item.List1Item {
        val vString1 = r.string()
        val vBranch1 = if (vString1 == "b") readCatalogPageOutList3ItemList1ItemBranch1Then(r) else readCatalogPageOutList3ItemList1ItemBranch1Else(r)
        return CatalogPageOut.List3Item.List1Item(vString1, vBranch1)
    }

    private fun readCatalogPageOutList3Item(r: PacketReader): CatalogPageOut.List3Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vBoolean2 = r.boolean()
        val vList1 = List(r.countInt(4)) { readCatalogPageOutList3ItemList1Item(r) }
        val vInt6 = r.int()
        val vBoolean3 = r.boolean()
        val vBoolean4 = r.boolean()
        val vString2 = r.string()
        return CatalogPageOut.List3Item(vInt1, vString1, vBoolean1, vInt2, vInt3, vInt4, vInt5, vBoolean2, vList1, vInt6, vBoolean3, vBoolean4, vString2)
    }

    private fun readCatalogPageOut(r: PacketReader): CatalogPageOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vList1 = List(r.countInt(2)) { r.string() }
        val vList2 = List(r.countInt(2)) { r.string() }
        val vList3 = List(r.countInt(36)) { readCatalogPageOutList3Item(r) }
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        val vRest = r.rest()
        return CatalogPageOut(vInt1, vString1, vString2, vList1, vList2, vList3, vInt2, vBoolean1, vRest)
    }

    private fun writeCatalogPageOutList3ItemList1ItemBranch1Then(w: PacketWriter, m: CatalogPageOut.List3Item.List1Item.Branch1.Then) {
        w.string(m.string1)
    }

    private fun writeCatalogPageOutList3ItemList1ItemBranch1ElseBranch1Then(w: PacketWriter, m: CatalogPageOut.List3Item.List1Item.Branch1.Else.Branch1.Then) {
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writeCatalogPageOutList3ItemList1ItemBranch1Else(w: PacketWriter, m: CatalogPageOut.List3Item.List1Item.Branch1.Else) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.boolean(m.boolean1)
        when (val b = m.branch1) {
            is CatalogPageOut.List3Item.List1Item.Branch1.Else.Branch1.Then -> writeCatalogPageOutList3ItemList1ItemBranch1ElseBranch1Then(w, b)
            is CatalogPageOut.List3Item.List1Item.Branch1.Else.Branch1.Else -> {}
        }
    }

    private fun writeCatalogPageOutList3ItemList1Item(w: PacketWriter, m: CatalogPageOut.List3Item.List1Item) {
        w.string(m.string1)
        when (val b = m.branch1) {
            is CatalogPageOut.List3Item.List1Item.Branch1.Then -> writeCatalogPageOutList3ItemList1ItemBranch1Then(w, b)
            is CatalogPageOut.List3Item.List1Item.Branch1.Else -> writeCatalogPageOutList3ItemList1ItemBranch1Else(w, b)
        }
    }

    private fun writeCatalogPageOutList3Item(w: PacketWriter, m: CatalogPageOut.List3Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.boolean(m.boolean1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.boolean(m.boolean2)
        w.countInt(m.list1.size)
        m.list1.forEach { writeCatalogPageOutList3ItemList1Item(w, it) }
        w.int(m.int6)
        w.boolean(m.boolean3)
        w.boolean(m.boolean4)
        w.string(m.string2)
    }

    private fun writeCatalogPageOut(w: PacketWriter, m: CatalogPageOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.countInt(m.list1.size)
        m.list1.forEach { w.string(it) }
        w.countInt(m.list2.size)
        m.list2.forEach { w.string(it) }
        w.countInt(m.list3.size)
        m.list3.forEach { writeCatalogPageOutList3Item(w, it) }
        w.int(m.int2)
        w.boolean(m.boolean1)
        w.bytes(m.rest)
    }
}
