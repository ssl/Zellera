package zel.protocol.generated

data class EnterOneWayDoorIn(
    val itemId: Int,
)

object EnterOneWayDoorInCodec : IncomingCodec<EnterOneWayDoorIn> {
    override val name = "EnterOneWayDoorIn"

    override fun read(r: PacketReader): EnterOneWayDoorIn = readEnterOneWayDoorIn(r)

    override fun write(w: PacketWriter, m: EnterOneWayDoorIn) = writeEnterOneWayDoorIn(w, m)

    private fun readEnterOneWayDoorIn(r: PacketReader): EnterOneWayDoorIn {
        val vItemId = r.int()
        return EnterOneWayDoorIn(vItemId)
    }

    private fun writeEnterOneWayDoorIn(w: PacketWriter, m: EnterOneWayDoorIn) {
        w.int(m.itemId)
    }
}
