package zel.protocol.generated

data class PlaceObjectIn(
    val placement: String,
)

object PlaceObjectInCodec : IncomingCodec<PlaceObjectIn> {
    override val name = "PlaceObjectIn"

    override fun read(r: PacketReader): PlaceObjectIn = readPlaceObjectIn(r)

    override fun write(w: PacketWriter, m: PlaceObjectIn) = writePlaceObjectIn(w, m)

    private fun readPlaceObjectIn(r: PacketReader): PlaceObjectIn {
        val vPlacement = r.string()
        return PlaceObjectIn(vPlacement)
    }

    private fun writePlaceObjectIn(w: PacketWriter, m: PlaceObjectIn) {
        w.string(m.placement)
    }
}
