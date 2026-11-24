package zel.protocol.generated

data class RoomsWithHighestScoreSearchIn(
    val adIndex: Int,
)

object RoomsWithHighestScoreSearchInCodec : IncomingCodec<RoomsWithHighestScoreSearchIn> {
    override val name = "RoomsWithHighestScoreSearchIn"

    override fun read(r: PacketReader): RoomsWithHighestScoreSearchIn = readRoomsWithHighestScoreSearchIn(r)

    override fun write(w: PacketWriter, m: RoomsWithHighestScoreSearchIn) = writeRoomsWithHighestScoreSearchIn(w, m)

    private fun readRoomsWithHighestScoreSearchIn(r: PacketReader): RoomsWithHighestScoreSearchIn {
        val vAdIndex = r.int()
        return RoomsWithHighestScoreSearchIn(vAdIndex)
    }

    private fun writeRoomsWithHighestScoreSearchIn(w: PacketWriter, m: RoomsWithHighestScoreSearchIn) {
        w.int(m.adIndex)
    }
}
