package zel.protocol.generated

data class DanceIn(
    val danceId: Int,
)

object DanceInCodec : IncomingCodec<DanceIn> {
    override val name = "DanceIn"

    override fun read(r: PacketReader): DanceIn = readDanceIn(r)

    override fun write(w: PacketWriter, m: DanceIn) = writeDanceIn(w, m)

    private fun readDanceIn(r: PacketReader): DanceIn {
        val vDanceId = r.int()
        return DanceIn(vDanceId)
    }

    private fun writeDanceIn(w: PacketWriter, m: DanceIn) {
        w.int(m.danceId)
    }
}
