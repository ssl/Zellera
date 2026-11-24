package zel.protocol.generated

data class NowPlayingOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
)

object NowPlayingOutCodec : OutgoingCodec<NowPlayingOut> {
    override val name = "NowPlayingOut"

    override fun read(r: PacketReader): NowPlayingOut = readNowPlayingOut(r)

    override fun write(w: PacketWriter, m: NowPlayingOut) = writeNowPlayingOut(w, m)

    private fun readNowPlayingOut(r: PacketReader): NowPlayingOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        return NowPlayingOut(vInt1, vInt2, vInt3, vInt4, vInt5)
    }

    private fun writeNowPlayingOut(w: PacketWriter, m: NowPlayingOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
    }
}
