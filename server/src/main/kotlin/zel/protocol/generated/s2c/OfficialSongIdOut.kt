package zel.protocol.generated

data class OfficialSongIdOut(
    val string1: String,
    val int1: Int,
)

object OfficialSongIdOutCodec : OutgoingCodec<OfficialSongIdOut> {
    override val name = "OfficialSongIdOut"

    override fun read(r: PacketReader): OfficialSongIdOut = readOfficialSongIdOut(r)

    override fun write(w: PacketWriter, m: OfficialSongIdOut) = writeOfficialSongIdOut(w, m)

    private fun readOfficialSongIdOut(r: PacketReader): OfficialSongIdOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        return OfficialSongIdOut(vString1, vInt1)
    }

    private fun writeOfficialSongIdOut(w: PacketWriter, m: OfficialSongIdOut) {
        w.string(m.string1)
        w.int(m.int1)
    }
}
