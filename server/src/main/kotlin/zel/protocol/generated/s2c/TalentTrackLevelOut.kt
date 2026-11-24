package zel.protocol.generated

data class TalentTrackLevelOut(
    val string1: String,
    val int1: Int,
    val int2: Int,
)

object TalentTrackLevelOutCodec : OutgoingCodec<TalentTrackLevelOut> {
    override val name = "TalentTrackLevelOut"

    override fun read(r: PacketReader): TalentTrackLevelOut = readTalentTrackLevelOut(r)

    override fun write(w: PacketWriter, m: TalentTrackLevelOut) = writeTalentTrackLevelOut(w, m)

    private fun readTalentTrackLevelOut(r: PacketReader): TalentTrackLevelOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        return TalentTrackLevelOut(vString1, vInt1, vInt2)
    }

    private fun writeTalentTrackLevelOut(w: PacketWriter, m: TalentTrackLevelOut) {
        w.string(m.string1)
        w.int(m.int1)
        w.int(m.int2)
    }
}
