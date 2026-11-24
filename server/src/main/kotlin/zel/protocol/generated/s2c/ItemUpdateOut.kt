package zel.protocol.generated

data class ItemUpdateOut(
    val string1: String,
    val int1: Int,
    val string2: String,
    val string3: String,
    val int2: Int,
    val int3: Int,
    val int4: Int,
)

object ItemUpdateOutCodec : OutgoingCodec<ItemUpdateOut> {
    override val name = "ItemUpdateOut"

    override fun read(r: PacketReader): ItemUpdateOut = readItemUpdateOut(r)

    override fun write(w: PacketWriter, m: ItemUpdateOut) = writeItemUpdateOut(w, m)

    private fun readItemUpdateOut(r: PacketReader): ItemUpdateOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        return ItemUpdateOut(vString1, vInt1, vString2, vString3, vInt2, vInt3, vInt4)
    }

    private fun writeItemUpdateOut(w: PacketWriter, m: ItemUpdateOut) {
        w.string(m.string1)
        w.int(m.int1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
    }
}
