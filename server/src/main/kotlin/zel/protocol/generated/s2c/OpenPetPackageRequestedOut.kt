package zel.protocol.generated

data class OpenPetPackageRequestedOut(
    val int1: Int,
    val trailing1: Trailing1?,
) {
    data class Trailing1(
        val int1: Int,
        val int2: Int,
        val string1: String,
        val int3: Int,
        val list1: List<List1Item>,
    ) {
        data class List1Item(
            val int1: Int,
            val int2: Int,
            val int3: Int,
        )
    }
}

object OpenPetPackageRequestedOutCodec : OutgoingCodec<OpenPetPackageRequestedOut> {
    override val name = "OpenPetPackageRequestedOut"

    override fun read(r: PacketReader): OpenPetPackageRequestedOut = readOpenPetPackageRequestedOut(r)

    override fun write(w: PacketWriter, m: OpenPetPackageRequestedOut) = writeOpenPetPackageRequestedOut(w, m)

    private fun readOpenPetPackageRequestedOutTrailing1List1Item(r: PacketReader): OpenPetPackageRequestedOut.Trailing1.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return OpenPetPackageRequestedOut.Trailing1.List1Item(vInt1, vInt2, vInt3)
    }

    private fun readOpenPetPackageRequestedOutTrailing1(r: PacketReader): OpenPetPackageRequestedOut.Trailing1 {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        val vList1 = List(r.countInt(12)) { readOpenPetPackageRequestedOutTrailing1List1Item(r) }
        return OpenPetPackageRequestedOut.Trailing1(vInt1, vInt2, vString1, vInt3, vList1)
    }

    private fun readOpenPetPackageRequestedOut(r: PacketReader): OpenPetPackageRequestedOut {
        val vInt1 = r.int()
        val vTrailing1 = if (r.remaining > 0) readOpenPetPackageRequestedOutTrailing1(r) else null
        return OpenPetPackageRequestedOut(vInt1, vTrailing1)
    }

    private fun writeOpenPetPackageRequestedOutTrailing1List1Item(w: PacketWriter, m: OpenPetPackageRequestedOut.Trailing1.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }

    private fun writeOpenPetPackageRequestedOutTrailing1(w: PacketWriter, m: OpenPetPackageRequestedOut.Trailing1) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
        w.countInt(m.list1.size)
        m.list1.forEach { writeOpenPetPackageRequestedOutTrailing1List1Item(w, it) }
    }

    private fun writeOpenPetPackageRequestedOut(w: PacketWriter, m: OpenPetPackageRequestedOut) {
        w.int(m.int1)
        m.trailing1?.let { writeOpenPetPackageRequestedOutTrailing1(w, it) }
    }
}
