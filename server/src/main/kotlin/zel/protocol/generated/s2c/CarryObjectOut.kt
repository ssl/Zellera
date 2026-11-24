package zel.protocol.generated

data class CarryObjectOut(
    val int1: Int,
    val int2: Int,
)

object CarryObjectOutCodec : OutgoingCodec<CarryObjectOut> {
    override val name = "CarryObjectOut"

    override fun read(r: PacketReader): CarryObjectOut = readCarryObjectOut(r)

    override fun write(w: PacketWriter, m: CarryObjectOut) = writeCarryObjectOut(w, m)

    private fun readCarryObjectOut(r: PacketReader): CarryObjectOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return CarryObjectOut(vInt1, vInt2)
    }

    private fun writeCarryObjectOut(w: PacketWriter, m: CarryObjectOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
