package zel.protocol.generated

data class Game2FullGameStatusOut(
    val body: ByteArray,
)

object Game2FullGameStatusOutCodec : OutgoingCodec<Game2FullGameStatusOut> {
    override val name = "Game2FullGameStatusOut"

    override fun read(r: PacketReader): Game2FullGameStatusOut = readGame2FullGameStatusOut(r)

    override fun write(w: PacketWriter, m: Game2FullGameStatusOut) = writeGame2FullGameStatusOut(w, m)

    private fun readGame2FullGameStatusOut(r: PacketReader): Game2FullGameStatusOut {
        val vBody = r.rest()
        return Game2FullGameStatusOut(vBody)
    }

    private fun writeGame2FullGameStatusOut(w: PacketWriter, m: Game2FullGameStatusOut) {
        w.bytes(m.body)
    }
}
