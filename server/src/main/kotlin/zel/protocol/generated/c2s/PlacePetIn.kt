package zel.protocol.generated

data class PlacePetIn(
    val petId: Int,
    val x: Int,
    val y: Int,
)

object PlacePetInCodec : IncomingCodec<PlacePetIn> {
    override val name = "PlacePetIn"

    override fun read(r: PacketReader): PlacePetIn = readPlacePetIn(r)

    override fun write(w: PacketWriter, m: PlacePetIn) = writePlacePetIn(w, m)

    private fun readPlacePetIn(r: PacketReader): PlacePetIn {
        val vPetId = r.int()
        val vX = r.int()
        val vY = r.int()
        return PlacePetIn(vPetId, vX, vY)
    }

    private fun writePlacePetIn(w: PacketWriter, m: PlacePetIn) {
        w.int(m.petId)
        w.int(m.x)
        w.int(m.y)
    }
}
