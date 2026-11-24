package zel.protocol.generated

data object Game2LeaveGameIn

object Game2LeaveGameInCodec : IncomingCodec<Game2LeaveGameIn> {
    override val name = "Game2LeaveGameIn"

    override fun read(r: PacketReader): Game2LeaveGameIn = Game2LeaveGameIn

    override fun write(w: PacketWriter, m: Game2LeaveGameIn) = writeGame2LeaveGameIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGame2LeaveGameIn(w: PacketWriter, m: Game2LeaveGameIn) {
    }
}
