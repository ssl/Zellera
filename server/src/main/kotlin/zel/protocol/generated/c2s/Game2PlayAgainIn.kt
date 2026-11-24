package zel.protocol.generated

data object Game2PlayAgainIn

object Game2PlayAgainInCodec : IncomingCodec<Game2PlayAgainIn> {
    override val name = "Game2PlayAgainIn"

    override fun read(r: PacketReader): Game2PlayAgainIn = Game2PlayAgainIn

    override fun write(w: PacketWriter, m: Game2PlayAgainIn) = writeGame2PlayAgainIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGame2PlayAgainIn(w: PacketWriter, m: Game2PlayAgainIn) {
    }
}
