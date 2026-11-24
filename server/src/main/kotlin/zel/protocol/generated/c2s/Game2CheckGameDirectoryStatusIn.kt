package zel.protocol.generated

data object Game2CheckGameDirectoryStatusIn

object Game2CheckGameDirectoryStatusInCodec : IncomingCodec<Game2CheckGameDirectoryStatusIn> {
    override val name = "Game2CheckGameDirectoryStatusIn"

    override fun read(r: PacketReader): Game2CheckGameDirectoryStatusIn = Game2CheckGameDirectoryStatusIn

    override fun write(w: PacketWriter, m: Game2CheckGameDirectoryStatusIn) = writeGame2CheckGameDirectoryStatusIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGame2CheckGameDirectoryStatusIn(w: PacketWriter, m: Game2CheckGameDirectoryStatusIn) {
    }
}
