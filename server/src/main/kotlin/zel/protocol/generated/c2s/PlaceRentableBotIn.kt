package zel.protocol.generated

data class PlaceRentableBotIn(
    val unitIndex: Int,
    val x: Int,
    val y: Int,
    val direction: Int,
)

object PlaceRentableBotInCodec : IncomingCodec<PlaceRentableBotIn> {
    override val name = "PlaceRentableBotIn"

    override fun read(r: PacketReader): PlaceRentableBotIn = readPlaceRentableBotIn(r)

    override fun write(w: PacketWriter, m: PlaceRentableBotIn) = writePlaceRentableBotIn(w, m)

    private fun readPlaceRentableBotIn(r: PacketReader): PlaceRentableBotIn {
        val vUnitIndex = r.int()
        val vX = r.int()
        val vY = r.int()
        val vDirection = r.int()
        return PlaceRentableBotIn(vUnitIndex, vX, vY, vDirection)
    }

    private fun writePlaceRentableBotIn(w: PacketWriter, m: PlaceRentableBotIn) {
        w.int(m.unitIndex)
        w.int(m.x)
        w.int(m.y)
        w.int(m.direction)
    }
}
