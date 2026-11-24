package zel.protocol.generated

data class FurniListRemoveMultipleOut(
    val stripIds: List<Int>,
)

object FurniListRemoveMultipleOutCodec : OutgoingCodec<FurniListRemoveMultipleOut> {
    override val name = "FurniListRemoveMultipleOut"

    override fun read(r: PacketReader): FurniListRemoveMultipleOut = readFurniListRemoveMultipleOut(r)

    override fun write(w: PacketWriter, m: FurniListRemoveMultipleOut) = writeFurniListRemoveMultipleOut(w, m)

    private fun readFurniListRemoveMultipleOut(r: PacketReader): FurniListRemoveMultipleOut {
        val vStripIds = List(r.countInt(4)) { r.int() }
        return FurniListRemoveMultipleOut(vStripIds)
    }

    private fun writeFurniListRemoveMultipleOut(w: PacketWriter, m: FurniListRemoveMultipleOut) {
        w.countInt(m.stripIds.size)
        m.stripIds.forEach { w.int(it) }
    }
}
