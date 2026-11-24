package zel.protocol.generated

data class Game2StageStartingOut(
    val body: ByteArray,
)

object Game2StageStartingOutCodec : OutgoingCodec<Game2StageStartingOut> {
    override val name = "Game2StageStartingOut"

    override fun read(r: PacketReader): Game2StageStartingOut = readGame2StageStartingOut(r)

    override fun write(w: PacketWriter, m: Game2StageStartingOut) = writeGame2StageStartingOut(w, m)

    private fun readGame2StageStartingOut(r: PacketReader): Game2StageStartingOut {
        val vBody = r.rest()
        return Game2StageStartingOut(vBody)
    }

    private fun writeGame2StageStartingOut(w: PacketWriter, m: Game2StageStartingOut) {
        w.bytes(m.body)
    }
}
