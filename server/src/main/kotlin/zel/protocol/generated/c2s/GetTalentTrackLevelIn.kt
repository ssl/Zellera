package zel.protocol.generated

data class GetTalentTrackLevelIn(
    val string1: String,
)

object GetTalentTrackLevelInCodec : IncomingCodec<GetTalentTrackLevelIn> {
    override val name = "GetTalentTrackLevelIn"

    override fun read(r: PacketReader): GetTalentTrackLevelIn = readGetTalentTrackLevelIn(r)

    override fun write(w: PacketWriter, m: GetTalentTrackLevelIn) = writeGetTalentTrackLevelIn(w, m)

    private fun readGetTalentTrackLevelIn(r: PacketReader): GetTalentTrackLevelIn {
        val vString1 = r.string()
        return GetTalentTrackLevelIn(vString1)
    }

    private fun writeGetTalentTrackLevelIn(w: PacketWriter, m: GetTalentTrackLevelIn) {
        w.string(m.string1)
    }
}
