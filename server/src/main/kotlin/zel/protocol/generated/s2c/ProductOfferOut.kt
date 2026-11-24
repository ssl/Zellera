package zel.protocol.generated

data class ProductOfferOut(
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

object ProductOfferOutCodec : OutgoingCodec<ProductOfferOut> {
    override val name = "ProductOfferOut"

    override fun read(r: PacketReader): ProductOfferOut = readProductOfferOut(r)

    override fun write(w: PacketWriter, m: ProductOfferOut) = writeProductOfferOut(w, m)

    private fun readProductOfferOutList1ItemBranch1Then(r: PacketReader): ProductOfferOut.List1Item.Branch1.Then {
        val vString1 = r.string()
        return ProductOfferOut.List1Item.Branch1.Then(vString1)
    }

    private fun readProductOfferOutList1ItemBranch1ElseBranch1Then(r: PacketReader): ProductOfferOut.List1Item.Branch1.Else.Branch1.Then {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return ProductOfferOut.List1Item.Branch1.Else.Branch1.Then(vInt1, vInt2)
    }

    private fun readProductOfferOutList1ItemBranch1Else(r: PacketReader): ProductOfferOut.List1Item.Branch1.Else {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        val vBranch1 = if (vBoolean1 == true) readProductOfferOutList1ItemBranch1ElseBranch1Then(r) else ProductOfferOut.List1Item.Branch1.Else.Branch1.Else
        return ProductOfferOut.List1Item.Branch1.Else(vInt1, vString1, vInt2, vBoolean1, vBranch1)
    }

    private fun readProductOfferOutList1Item(r: PacketReader): ProductOfferOut.List1Item {
        val vString1 = r.string()
        val vBranch1 = if (vString1 == "b") readProductOfferOutList1ItemBranch1Then(r) else readProductOfferOutList1ItemBranch1Else(r)
        return ProductOfferOut.List1Item(vString1, vBranch1)
    }

    private fun readProductOfferOut(r: PacketReader): ProductOfferOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vBoolean2 = r.boolean()
        val vList1 = List(r.countInt(4)) { readProductOfferOutList1Item(r) }
        val vInt6 = r.int()
        val vBoolean3 = r.boolean()
        val vBoolean4 = r.boolean()
        val vString2 = r.string()
        return ProductOfferOut(vInt1, vString1, vBoolean1, vInt2, vInt3, vInt4, vInt5, vBoolean2, vList1, vInt6, vBoolean3, vBoolean4, vString2)
    }

    private fun writeProductOfferOutList1ItemBranch1Then(w: PacketWriter, m: ProductOfferOut.List1Item.Branch1.Then) {
        w.string(m.string1)
    }

    private fun writeProductOfferOutList1ItemBranch1ElseBranch1Then(w: PacketWriter, m: ProductOfferOut.List1Item.Branch1.Else.Branch1.Then) {
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writeProductOfferOutList1ItemBranch1Else(w: PacketWriter, m: ProductOfferOut.List1Item.Branch1.Else) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.boolean(m.boolean1)
        when (val b = m.branch1) {
            is ProductOfferOut.List1Item.Branch1.Else.Branch1.Then -> writeProductOfferOutList1ItemBranch1ElseBranch1Then(w, b)
            is ProductOfferOut.List1Item.Branch1.Else.Branch1.Else -> {}
        }
    }

    private fun writeProductOfferOutList1Item(w: PacketWriter, m: ProductOfferOut.List1Item) {
        w.string(m.string1)
        when (val b = m.branch1) {
            is ProductOfferOut.List1Item.Branch1.Then -> writeProductOfferOutList1ItemBranch1Then(w, b)
            is ProductOfferOut.List1Item.Branch1.Else -> writeProductOfferOutList1ItemBranch1Else(w, b)
        }
    }

    private fun writeProductOfferOut(w: PacketWriter, m: ProductOfferOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.boolean(m.boolean1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.boolean(m.boolean2)
        w.countInt(m.list1.size)
        m.list1.forEach { writeProductOfferOutList1Item(w, it) }
        w.int(m.int6)
        w.boolean(m.boolean3)
        w.boolean(m.boolean4)
        w.string(m.string2)
    }
}
