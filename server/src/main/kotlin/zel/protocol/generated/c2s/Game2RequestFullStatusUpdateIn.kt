package zel.protocol.generated

data class Game2RequestFullStatusUpdateIn(
    val reason: Int,
)

object Game2RequestFullStatusUpdateInCodec : IncomingCodec<Game2RequestFullStatusUpdateIn> {
    override val name = "Game2RequestFullStatusUpdateIn"

    override fun read(r: PacketReader): Game2RequestFullStatusUpdateIn = readGame2RequestFullStatusUpdateIn(r)

    override fun write(w: PacketWriter, m: Game2RequestFullStatusUpdateIn) = writeGame2RequestFullStatusUpdateIn(w, m)

    private fun readGame2RequestFullStatusUpdateIn(r: PacketReader): Game2RequestFullStatusUpdateIn {
        val vReason = r.int()
        return Game2RequestFullStatusUpdateIn(vReason)
    }

    private fun writeGame2RequestFullStatusUpdateIn(w: PacketWriter, m: Game2RequestFullStatusUpdateIn) {
        w.int(m.reason)
    }
}
