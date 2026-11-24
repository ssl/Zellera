package zel.protocol.generated

data class Game2GetAccountGameStatusIn(
    val gameTypeId: Int,
)

object Game2GetAccountGameStatusInCodec : IncomingCodec<Game2GetAccountGameStatusIn> {
    override val name = "Game2GetAccountGameStatusIn"

    override fun read(r: PacketReader): Game2GetAccountGameStatusIn = readGame2GetAccountGameStatusIn(r)

    override fun write(w: PacketWriter, m: Game2GetAccountGameStatusIn) = writeGame2GetAccountGameStatusIn(w, m)

    private fun readGame2GetAccountGameStatusIn(r: PacketReader): Game2GetAccountGameStatusIn {
        val vGameTypeId = r.int()
        return Game2GetAccountGameStatusIn(vGameTypeId)
    }

    private fun writeGame2GetAccountGameStatusIn(w: PacketWriter, m: Game2GetAccountGameStatusIn) {
        w.int(m.gameTypeId)
    }
}
