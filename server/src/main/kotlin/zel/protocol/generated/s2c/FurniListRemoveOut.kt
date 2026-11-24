package zel.protocol.generated

data class FurniListRemoveOut(
    val stripId: Int,
)

object FurniListRemoveOutCodec : OutgoingCodec<FurniListRemoveOut> {
    override val name = "FurniListRemoveOut"

    override fun read(r: PacketReader): FurniListRemoveOut = readFurniListRemoveOut(r)

    override fun write(w: PacketWriter, m: FurniListRemoveOut) = writeFurniListRemoveOut(w, m)

    private fun readFurniListRemoveOut(r: PacketReader): FurniListRemoveOut {
        val vStripId = r.int()
        return FurniListRemoveOut(vStripId)
    }

    private fun writeFurniListRemoveOut(w: PacketWriter, m: FurniListRemoveOut) {
        w.int(m.stripId)
    }
}
