package zel.protocol.generated

data class ItemAddOut(
    val string1: String,
    val int1: Int,
    val string2: String,
    val string3: String,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val string4: String,
)

object ItemAddOutCodec : OutgoingCodec<ItemAddOut> {
    override val name = "ItemAddOut"

    override fun read(r: PacketReader): ItemAddOut = readItemAddOut(r)

    override fun write(w: PacketWriter, m: ItemAddOut) = writeItemAddOut(w, m)

    private fun readItemAddOut(r: PacketReader): ItemAddOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vString4 = r.string()
        return ItemAddOut(vString1, vInt1, vString2, vString3, vInt2, vInt3, vInt4, vString4)
    }

    private fun writeItemAddOut(w: PacketWriter, m: ItemAddOut) {
        w.string(m.string1)
        w.int(m.int1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.string(m.string4)
    }
}
