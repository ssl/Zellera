package zel.protocol.generated

data class GetTalentTrackIn(
    val string1: String,
)

object GetTalentTrackInCodec : IncomingCodec<GetTalentTrackIn> {
    override val name = "GetTalentTrackIn"

    override fun read(r: PacketReader): GetTalentTrackIn = readGetTalentTrackIn(r)

    override fun write(w: PacketWriter, m: GetTalentTrackIn) = writeGetTalentTrackIn(w, m)

    private fun readGetTalentTrackIn(r: PacketReader): GetTalentTrackIn {
        val vString1 = r.string()
        return GetTalentTrackIn(vString1)
    }

    private fun writeGetTalentTrackIn(w: PacketWriter, m: GetTalentTrackIn) {
        w.string(m.string1)
    }
}
