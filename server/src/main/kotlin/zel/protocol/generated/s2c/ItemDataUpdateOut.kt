package zel.protocol.generated

data class ItemDataUpdateOut(
    val string1: String,
    val string2: String,
)

object ItemDataUpdateOutCodec : OutgoingCodec<ItemDataUpdateOut> {
    override val name = "ItemDataUpdateOut"

    override fun read(r: PacketReader): ItemDataUpdateOut = readItemDataUpdateOut(r)

    override fun write(w: PacketWriter, m: ItemDataUpdateOut) = writeItemDataUpdateOut(w, m)

    private fun readItemDataUpdateOut(r: PacketReader): ItemDataUpdateOut {
        val vString1 = r.string()
        val vString2 = r.string()
        return ItemDataUpdateOut(vString1, vString2)
    }

    private fun writeItemDataUpdateOut(w: PacketWriter, m: ItemDataUpdateOut) {
        w.string(m.string1)
        w.string(m.string2)
    }
}
