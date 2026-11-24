package zel.protocol.generated

data object Game2GameNotFoundOut

object Game2GameNotFoundOutCodec : OutgoingCodec<Game2GameNotFoundOut> {
    override val name = "Game2GameNotFoundOut"

    override fun read(r: PacketReader): Game2GameNotFoundOut = Game2GameNotFoundOut

    override fun write(w: PacketWriter, m: Game2GameNotFoundOut) = writeGame2GameNotFoundOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGame2GameNotFoundOut(w: PacketWriter, m: Game2GameNotFoundOut) {
    }
}
