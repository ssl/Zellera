package zel.protocol.generated

data class PlacePostItIn(
    val itemId: Int,
    val location: String,
)

object PlacePostItInCodec : IncomingCodec<PlacePostItIn> {
    override val name = "PlacePostItIn"

    override fun read(r: PacketReader): PlacePostItIn = readPlacePostItIn(r)

    override fun write(w: PacketWriter, m: PlacePostItIn) = writePlacePostItIn(w, m)

    private fun readPlacePostItIn(r: PacketReader): PlacePostItIn {
        val vItemId = r.int()
        val vLocation = r.string()
        return PlacePostItIn(vItemId, vLocation)
    }

    private fun writePlacePostItIn(w: PacketWriter, m: PlacePostItIn) {
        w.int(m.itemId)
        w.string(m.location)
    }
}
