package zel.protocol.generated

data class RateFlatIn(
    val points: Int,
)

object RateFlatInCodec : IncomingCodec<RateFlatIn> {
    override val name = "RateFlatIn"

    override fun read(r: PacketReader): RateFlatIn = readRateFlatIn(r)

    override fun write(w: PacketWriter, m: RateFlatIn) = writeRateFlatIn(w, m)

    private fun readRateFlatIn(r: PacketReader): RateFlatIn {
        val vPoints = r.int()
        return RateFlatIn(vPoints)
    }

    private fun writeRateFlatIn(w: PacketWriter, m: RateFlatIn) {
        w.int(m.points)
    }
}
