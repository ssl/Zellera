package zel.protocol.generated

data object Game2GameCancelledOut

object Game2GameCancelledOutCodec : OutgoingCodec<Game2GameCancelledOut> {
    override val name = "Game2GameCancelledOut"

    override fun read(r: PacketReader): Game2GameCancelledOut = Game2GameCancelledOut

    override fun write(w: PacketWriter, m: Game2GameCancelledOut) = writeGame2GameCancelledOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGame2GameCancelledOut(w: PacketWriter, m: Game2GameCancelledOut) {
    }
}
