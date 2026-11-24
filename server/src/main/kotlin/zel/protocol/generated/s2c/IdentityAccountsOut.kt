package zel.protocol.generated

data class IdentityAccountsOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
    )
}

object IdentityAccountsOutCodec : OutgoingCodec<IdentityAccountsOut> {
    override val name = "IdentityAccountsOut"

    override fun read(r: PacketReader): IdentityAccountsOut = readIdentityAccountsOut(r)

    override fun write(w: PacketWriter, m: IdentityAccountsOut) = writeIdentityAccountsOut(w, m)

    private fun readIdentityAccountsOutList1Item(r: PacketReader): IdentityAccountsOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        return IdentityAccountsOut.List1Item(vInt1, vString1)
    }

    private fun readIdentityAccountsOut(r: PacketReader): IdentityAccountsOut {
        val vList1 = List(r.countInt(6)) { readIdentityAccountsOutList1Item(r) }
        return IdentityAccountsOut(vList1)
    }

    private fun writeIdentityAccountsOutList1Item(w: PacketWriter, m: IdentityAccountsOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeIdentityAccountsOut(w: PacketWriter, m: IdentityAccountsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeIdentityAccountsOutList1Item(w, it) }
    }
}
