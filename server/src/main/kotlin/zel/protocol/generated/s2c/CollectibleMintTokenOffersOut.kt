package zel.protocol.generated

data class CollectibleMintTokenOffersOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val int2: Int,
        val int3: Int,
    )
}

object CollectibleMintTokenOffersOutCodec : OutgoingCodec<CollectibleMintTokenOffersOut> {
    override val name = "CollectibleMintTokenOffersOut"

    override fun read(r: PacketReader): CollectibleMintTokenOffersOut = readCollectibleMintTokenOffersOut(r)

    override fun write(w: PacketWriter, m: CollectibleMintTokenOffersOut) = writeCollectibleMintTokenOffersOut(w, m)

    private fun readCollectibleMintTokenOffersOutList1Item(r: PacketReader): CollectibleMintTokenOffersOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return CollectibleMintTokenOffersOut.List1Item(vInt1, vString1, vInt2, vInt3)
    }

    private fun readCollectibleMintTokenOffersOut(r: PacketReader): CollectibleMintTokenOffersOut {
        val vList1 = List(r.countInt(14)) { readCollectibleMintTokenOffersOutList1Item(r) }
        return CollectibleMintTokenOffersOut(vList1)
    }

    private fun writeCollectibleMintTokenOffersOutList1Item(w: PacketWriter, m: CollectibleMintTokenOffersOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
    }

    private fun writeCollectibleMintTokenOffersOut(w: PacketWriter, m: CollectibleMintTokenOffersOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeCollectibleMintTokenOffersOutList1Item(w, it) }
    }
}
