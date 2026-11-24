package zel.protocol.generated

data class PerkAllowancesOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val string1: String,
        val string2: String,
        val boolean1: Boolean,
    )
}

object PerkAllowancesOutCodec : OutgoingCodec<PerkAllowancesOut> {
    override val name = "PerkAllowancesOut"

    override fun read(r: PacketReader): PerkAllowancesOut = readPerkAllowancesOut(r)

    override fun write(w: PacketWriter, m: PerkAllowancesOut) = writePerkAllowancesOut(w, m)

    private fun readPerkAllowancesOutList1Item(r: PacketReader): PerkAllowancesOut.List1Item {
        val vString1 = r.string()
        val vString2 = r.string()
        val vBoolean1 = r.boolean()
        return PerkAllowancesOut.List1Item(vString1, vString2, vBoolean1)
    }

    private fun readPerkAllowancesOut(r: PacketReader): PerkAllowancesOut {
        val vList1 = List(r.countInt(5)) { readPerkAllowancesOutList1Item(r) }
        return PerkAllowancesOut(vList1)
    }

    private fun writePerkAllowancesOutList1Item(w: PacketWriter, m: PerkAllowancesOut.List1Item) {
        w.string(m.string1)
        w.string(m.string2)
        w.boolean(m.boolean1)
    }

    private fun writePerkAllowancesOut(w: PacketWriter, m: PerkAllowancesOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writePerkAllowancesOutList1Item(w, it) }
    }
}
