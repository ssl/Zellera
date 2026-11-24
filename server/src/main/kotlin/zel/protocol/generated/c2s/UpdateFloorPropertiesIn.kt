package zel.protocol.generated

data class UpdateFloorPropertiesIn(
    val rest: ByteArray,
)

object UpdateFloorPropertiesInCodec : IncomingCodec<UpdateFloorPropertiesIn> {
    override val name = "UpdateFloorPropertiesIn"

    override fun read(r: PacketReader): UpdateFloorPropertiesIn = readUpdateFloorPropertiesIn(r)

    override fun write(w: PacketWriter, m: UpdateFloorPropertiesIn) = writeUpdateFloorPropertiesIn(w, m)

    private fun readUpdateFloorPropertiesIn(r: PacketReader): UpdateFloorPropertiesIn {
        val vRest = r.rest()
        return UpdateFloorPropertiesIn(vRest)
    }

    private fun writeUpdateFloorPropertiesIn(w: PacketWriter, m: UpdateFloorPropertiesIn) {
        w.bytes(m.rest)
    }
}
