package zel.protocol.generated

data class ItemRemoveOut(
    val string1: String,
    val int1: Int,
    val boolean1: Boolean,
)

object ItemRemoveOutCodec : OutgoingCodec<ItemRemoveOut> {
    override val name = "ItemRemoveOut"

    override fun read(r: PacketReader): ItemRemoveOut = readItemRemoveOut(r)

    override fun write(w: PacketWriter, m: ItemRemoveOut) = writeItemRemoveOut(w, m)

    private fun readItemRemoveOut(r: PacketReader): ItemRemoveOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        return ItemRemoveOut(vString1, vInt1, vBoolean1)
    }

    private fun writeItemRemoveOut(w: PacketWriter, m: ItemRemoveOut) {
        w.string(m.string1)
        w.int(m.int1)
        w.boolean(m.boolean1)
    }
}
