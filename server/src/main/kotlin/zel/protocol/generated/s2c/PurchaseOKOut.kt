package zel.protocol.generated

data class PurchaseOKOut(
    val int1: Int,
    val string1: String,
    val boolean1: Boolean,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val boolean2: Boolean,
    val list1: List<List1Item>,
    val int5: Int,
    val boolean3: Boolean,
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

object PurchaseOKOutCodec : OutgoingCodec<PurchaseOKOut> {
    override val name = "PurchaseOKOut"

    override fun read(r: PacketReader): PurchaseOKOut = readPurchaseOKOut(r)

    override fun write(w: PacketWriter, m: PurchaseOKOut) = writePurchaseOKOut(w, m)

    private fun readPurchaseOKOutList1ItemBranch1Then(r: PacketReader): PurchaseOKOut.List1Item.Branch1.Then {
        val vString1 = r.string()
        return PurchaseOKOut.List1Item.Branch1.Then(vString1)
    }

    private fun readPurchaseOKOutList1ItemBranch1ElseBranch1Then(r: PacketReader): PurchaseOKOut.List1Item.Branch1.Else.Branch1.Then {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return PurchaseOKOut.List1Item.Branch1.Else.Branch1.Then(vInt1, vInt2)
    }

    private fun readPurchaseOKOutList1ItemBranch1Else(r: PacketReader): PurchaseOKOut.List1Item.Branch1.Else {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        val vBranch1 = if (vBoolean1 == true) readPurchaseOKOutList1ItemBranch1ElseBranch1Then(r) else PurchaseOKOut.List1Item.Branch1.Else.Branch1.Else
        return PurchaseOKOut.List1Item.Branch1.Else(vInt1, vString1, vInt2, vBoolean1, vBranch1)
    }

    private fun readPurchaseOKOutList1Item(r: PacketReader): PurchaseOKOut.List1Item {
        val vString1 = r.string()
        val vBranch1 = if (vString1 == "b") readPurchaseOKOutList1ItemBranch1Then(r) else readPurchaseOKOutList1ItemBranch1Else(r)
        return PurchaseOKOut.List1Item(vString1, vBranch1)
    }

    private fun readPurchaseOKOut(r: PacketReader): PurchaseOKOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vBoolean2 = r.boolean()
        val vList1 = List(r.countInt(4)) { readPurchaseOKOutList1Item(r) }
        val vInt5 = r.int()
        val vBoolean3 = r.boolean()
        return PurchaseOKOut(vInt1, vString1, vBoolean1, vInt2, vInt3, vInt4, vBoolean2, vList1, vInt5, vBoolean3)
    }

    private fun writePurchaseOKOutList1ItemBranch1Then(w: PacketWriter, m: PurchaseOKOut.List1Item.Branch1.Then) {
        w.string(m.string1)
    }

    private fun writePurchaseOKOutList1ItemBranch1ElseBranch1Then(w: PacketWriter, m: PurchaseOKOut.List1Item.Branch1.Else.Branch1.Then) {
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writePurchaseOKOutList1ItemBranch1Else(w: PacketWriter, m: PurchaseOKOut.List1Item.Branch1.Else) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.boolean(m.boolean1)
        when (val b = m.branch1) {
            is PurchaseOKOut.List1Item.Branch1.Else.Branch1.Then -> writePurchaseOKOutList1ItemBranch1ElseBranch1Then(w, b)
            is PurchaseOKOut.List1Item.Branch1.Else.Branch1.Else -> {}
        }
    }

    private fun writePurchaseOKOutList1Item(w: PacketWriter, m: PurchaseOKOut.List1Item) {
        w.string(m.string1)
        when (val b = m.branch1) {
            is PurchaseOKOut.List1Item.Branch1.Then -> writePurchaseOKOutList1ItemBranch1Then(w, b)
            is PurchaseOKOut.List1Item.Branch1.Else -> writePurchaseOKOutList1ItemBranch1Else(w, b)
        }
    }

    private fun writePurchaseOKOut(w: PacketWriter, m: PurchaseOKOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.boolean(m.boolean1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.boolean(m.boolean2)
        w.countInt(m.list1.size)
        m.list1.forEach { writePurchaseOKOutList1Item(w, it) }
        w.int(m.int5)
        w.boolean(m.boolean3)
    }
}
