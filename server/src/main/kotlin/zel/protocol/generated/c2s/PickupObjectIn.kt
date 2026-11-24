package zel.protocol.generated

data class PickupObjectIn(
    val category: Int,
    val itemId: Int,
    val confirmed: Boolean,
)

object PickupObjectInCodec : IncomingCodec<PickupObjectIn> {
    override val name = "PickupObjectIn"

    override fun read(r: PacketReader): PickupObjectIn = readPickupObjectIn(r)

    override fun write(w: PacketWriter, m: PickupObjectIn) = writePickupObjectIn(w, m)

    private fun readPickupObjectIn(r: PacketReader): PickupObjectIn {
        val vCategory = r.int()
        val vItemId = r.int()
        val vConfirmed = r.boolean()
        return PickupObjectIn(vCategory, vItemId, vConfirmed)
    }

    private fun writePickupObjectIn(w: PacketWriter, m: PickupObjectIn) {
        w.int(m.category)
        w.int(m.itemId)
        w.boolean(m.confirmed)
    }
}
