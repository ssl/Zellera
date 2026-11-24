package zel.protocol.generated

data class Game2ExitGameIn(
    val flag: Boolean,
)

object Game2ExitGameInCodec : IncomingCodec<Game2ExitGameIn> {
    override val name = "Game2ExitGameIn"

    override fun read(r: PacketReader): Game2ExitGameIn = readGame2ExitGameIn(r)

    override fun write(w: PacketWriter, m: Game2ExitGameIn) = writeGame2ExitGameIn(w, m)

    private fun readGame2ExitGameIn(r: PacketReader): Game2ExitGameIn {
        val vFlag = r.boolean()
        return Game2ExitGameIn(vFlag)
    }

    private fun writeGame2ExitGameIn(w: PacketWriter, m: Game2ExitGameIn) {
        w.boolean(m.flag)
    }
}
