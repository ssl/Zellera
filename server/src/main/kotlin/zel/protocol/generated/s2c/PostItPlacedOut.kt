package zel.protocol.generated

data class PostItPlacedOut(
    val int1: Int,
    val int2: Int,
)

object PostItPlacedOutCodec : OutgoingCodec<PostItPlacedOut> {
    override val name = "PostItPlacedOut"

    override fun read(r: PacketReader): PostItPlacedOut = readPostItPlacedOut(r)

    override fun write(w: PacketWriter, m: PostItPlacedOut) = writePostItPlacedOut(w, m)

    private fun readPostItPlacedOut(r: PacketReader): PostItPlacedOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return PostItPlacedOut(vInt1, vInt2)
    }

    private fun writePostItPlacedOut(w: PacketWriter, m: PostItPlacedOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
