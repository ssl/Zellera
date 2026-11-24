package zel.protocol.generated

data class GetOfficialSongIdIn(
    val songName: String,
)

object GetOfficialSongIdInCodec : IncomingCodec<GetOfficialSongIdIn> {
    override val name = "GetOfficialSongIdIn"

    override fun read(r: PacketReader): GetOfficialSongIdIn = readGetOfficialSongIdIn(r)

    override fun write(w: PacketWriter, m: GetOfficialSongIdIn) = writeGetOfficialSongIdIn(w, m)

    private fun readGetOfficialSongIdIn(r: PacketReader): GetOfficialSongIdIn {
        val vSongName = r.string()
        return GetOfficialSongIdIn(vSongName)
    }

    private fun writeGetOfficialSongIdIn(w: PacketWriter, m: GetOfficialSongIdIn) {
        w.string(m.songName)
    }
}
