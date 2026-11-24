package zel.protocol.generated

data class GetSongInfoIn(
    val songIds: List<Int>,
)

object GetSongInfoInCodec : IncomingCodec<GetSongInfoIn> {
    override val name = "GetSongInfoIn"

    override fun read(r: PacketReader): GetSongInfoIn = readGetSongInfoIn(r)

    override fun write(w: PacketWriter, m: GetSongInfoIn) = writeGetSongInfoIn(w, m)

    private fun readGetSongInfoIn(r: PacketReader): GetSongInfoIn {
        val vSongIds = List(r.countInt(4)) { r.int() }
        return GetSongInfoIn(vSongIds)
    }

    private fun writeGetSongInfoIn(w: PacketWriter, m: GetSongInfoIn) {
        w.countInt(m.songIds.size)
        m.songIds.forEach { w.int(it) }
    }
}
