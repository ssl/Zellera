package zel.protocol.generated

data class ItemStateUpdateOut(
    val int1: Int,
    val string1: String,
)

object ItemStateUpdateOutCodec : OutgoingCodec<ItemStateUpdateOut> {
    override val name = "ItemStateUpdateOut"

    override fun read(r: PacketReader): ItemStateUpdateOut = readItemStateUpdateOut(r)

    override fun write(w: PacketWriter, m: ItemStateUpdateOut) = writeItemStateUpdateOut(w, m)

    private fun readItemStateUpdateOut(r: PacketReader): ItemStateUpdateOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        return ItemStateUpdateOut(vInt1, vString1)
    }

    private fun writeItemStateUpdateOut(w: PacketWriter, m: ItemStateUpdateOut) {
        w.int(m.int1)
        w.string(m.string1)
    }
}
