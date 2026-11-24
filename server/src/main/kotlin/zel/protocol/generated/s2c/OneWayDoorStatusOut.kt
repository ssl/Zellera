package zel.protocol.generated

data class OneWayDoorStatusOut(
    val int1: Int,
    val int2: Int,
)

object OneWayDoorStatusOutCodec : OutgoingCodec<OneWayDoorStatusOut> {
    override val name = "OneWayDoorStatusOut"

    override fun read(r: PacketReader): OneWayDoorStatusOut = readOneWayDoorStatusOut(r)

    override fun write(w: PacketWriter, m: OneWayDoorStatusOut) = writeOneWayDoorStatusOut(w, m)

    private fun readOneWayDoorStatusOut(r: PacketReader): OneWayDoorStatusOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return OneWayDoorStatusOut(vInt1, vInt2)
    }

    private fun writeOneWayDoorStatusOut(w: PacketWriter, m: OneWayDoorStatusOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
