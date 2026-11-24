package zel.protocol.generated

data class GetCollectorScoreIn(
    val string1: String,
)

object GetCollectorScoreInCodec : IncomingCodec<GetCollectorScoreIn> {
    override val name = "GetCollectorScoreIn"

    override fun read(r: PacketReader): GetCollectorScoreIn = readGetCollectorScoreIn(r)

    override fun write(w: PacketWriter, m: GetCollectorScoreIn) = writeGetCollectorScoreIn(w, m)

    private fun readGetCollectorScoreIn(r: PacketReader): GetCollectorScoreIn {
        val vString1 = r.string()
        return GetCollectorScoreIn(vString1)
    }

    private fun writeGetCollectorScoreIn(w: PacketWriter, m: GetCollectorScoreIn) {
        w.string(m.string1)
    }
}
