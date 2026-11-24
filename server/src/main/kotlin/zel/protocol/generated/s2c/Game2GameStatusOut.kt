package zel.protocol.generated

data class Game2GameStatusOut(
    val body: ByteArray,
)

object Game2GameStatusOutCodec : OutgoingCodec<Game2GameStatusOut> {
    override val name = "Game2GameStatusOut"

    override fun read(r: PacketReader): Game2GameStatusOut = readGame2GameStatusOut(r)

    override fun write(w: PacketWriter, m: Game2GameStatusOut) = writeGame2GameStatusOut(w, m)

    private fun readGame2GameStatusOut(r: PacketReader): Game2GameStatusOut {
        val vBody = r.rest()
        return Game2GameStatusOut(vBody)
    }

    private fun writeGame2GameStatusOut(w: PacketWriter, m: Game2GameStatusOut) {
        w.bytes(m.body)
    }
}
