package zel.protocol.generated

data class PlayListSongAddedOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val string2: String,
)

object PlayListSongAddedOutCodec : OutgoingCodec<PlayListSongAddedOut> {
    override val name = "PlayListSongAddedOut"

    override fun read(r: PacketReader): PlayListSongAddedOut = readPlayListSongAddedOut(r)

    override fun write(w: PacketWriter, m: PlayListSongAddedOut) = writePlayListSongAddedOut(w, m)

    private fun readPlayListSongAddedOut(r: PacketReader): PlayListSongAddedOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        return PlayListSongAddedOut(vInt1, vInt2, vString1, vString2)
    }

    private fun writePlayListSongAddedOut(w: PacketWriter, m: PlayListSongAddedOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.string(m.string2)
    }
}
