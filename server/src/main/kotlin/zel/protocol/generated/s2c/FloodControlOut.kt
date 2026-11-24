package zel.protocol.generated

data class FloodControlOut(
    val int1: Int,
)

object FloodControlOutCodec : OutgoingCodec<FloodControlOut> {
    override val name = "FloodControlOut"

    override fun read(r: PacketReader): FloodControlOut = readFloodControlOut(r)

    override fun write(w: PacketWriter, m: FloodControlOut) = writeFloodControlOut(w, m)

    private fun readFloodControlOut(r: PacketReader): FloodControlOut {
        val vInt1 = r.int()
        return FloodControlOut(vInt1)
    }

    private fun writeFloodControlOut(w: PacketWriter, m: FloodControlOut) {
        w.int(m.int1)
    }
}
