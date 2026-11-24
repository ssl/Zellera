package zel.protocol.generated

data class LookToIn(
    val x: Int,
    val y: Int,
)

object LookToInCodec : IncomingCodec<LookToIn> {
    override val name = "LookToIn"

    override fun read(r: PacketReader): LookToIn = readLookToIn(r)

    override fun write(w: PacketWriter, m: LookToIn) = writeLookToIn(w, m)

    private fun readLookToIn(r: PacketReader): LookToIn {
        val vX = r.int()
        val vY = r.int()
        return LookToIn(vX, vY)
    }

    private fun writeLookToIn(w: PacketWriter, m: LookToIn) {
        w.int(m.x)
        w.int(m.y)
    }
}
