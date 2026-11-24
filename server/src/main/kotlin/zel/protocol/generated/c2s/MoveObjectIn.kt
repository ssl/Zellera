package zel.protocol.generated

data class MoveObjectIn(
    val furniId: Int,
    val x: Int,
    val y: Int,
    val rotation: Int,
)

object MoveObjectInCodec : IncomingCodec<MoveObjectIn> {
    override val name = "MoveObjectIn"

    override fun read(r: PacketReader): MoveObjectIn = readMoveObjectIn(r)

    override fun write(w: PacketWriter, m: MoveObjectIn) = writeMoveObjectIn(w, m)

    private fun readMoveObjectIn(r: PacketReader): MoveObjectIn {
        val vFurniId = r.int()
        val vX = r.int()
        val vY = r.int()
        val vRotation = r.int()
        return MoveObjectIn(vFurniId, vX, vY, vRotation)
    }

    private fun writeMoveObjectIn(w: PacketWriter, m: MoveObjectIn) {
        w.int(m.furniId)
        w.int(m.x)
        w.int(m.y)
        w.int(m.rotation)
    }
}
