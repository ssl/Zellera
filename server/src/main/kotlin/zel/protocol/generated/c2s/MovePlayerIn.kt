package zel.protocol.generated

data class MovePlayerIn(
    val x: Int,
    val y: Int,
)

object MovePlayerInCodec : IncomingCodec<MovePlayerIn> {
    override val name = "MovePlayerIn"

    override fun read(r: PacketReader): MovePlayerIn = readMovePlayerIn(r)

    override fun write(w: PacketWriter, m: MovePlayerIn) = writeMovePlayerIn(w, m)

    private fun readMovePlayerIn(r: PacketReader): MovePlayerIn {
        val vX = r.int()
        val vY = r.int()
        return MovePlayerIn(vX, vY)
    }

    private fun writeMovePlayerIn(w: PacketWriter, m: MovePlayerIn) {
        w.int(m.x)
        w.int(m.y)
    }
}
