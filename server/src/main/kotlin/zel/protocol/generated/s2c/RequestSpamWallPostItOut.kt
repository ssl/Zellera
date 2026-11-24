package zel.protocol.generated

data class RequestSpamWallPostItOut(
    val int1: Int,
    val string1: String,
)

object RequestSpamWallPostItOutCodec : OutgoingCodec<RequestSpamWallPostItOut> {
    override val name = "RequestSpamWallPostItOut"

    override fun read(r: PacketReader): RequestSpamWallPostItOut = readRequestSpamWallPostItOut(r)

    override fun write(w: PacketWriter, m: RequestSpamWallPostItOut) = writeRequestSpamWallPostItOut(w, m)

    private fun readRequestSpamWallPostItOut(r: PacketReader): RequestSpamWallPostItOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        return RequestSpamWallPostItOut(vInt1, vString1)
    }

    private fun writeRequestSpamWallPostItOut(w: PacketWriter, m: RequestSpamWallPostItOut) {
        w.int(m.int1)
        w.string(m.string1)
    }
}
