package zel.protocol.generated

data class GroupDeactivatedOut(
    val int1: Int,
)

object GroupDeactivatedOutCodec : OutgoingCodec<GroupDeactivatedOut> {
    override val name = "GroupDeactivatedOut"

    override fun read(r: PacketReader): GroupDeactivatedOut = readGroupDeactivatedOut(r)

    override fun write(w: PacketWriter, m: GroupDeactivatedOut) = writeGroupDeactivatedOut(w, m)

    private fun readGroupDeactivatedOut(r: PacketReader): GroupDeactivatedOut {
        val vInt1 = r.int()
        return GroupDeactivatedOut(vInt1)
    }

    private fun writeGroupDeactivatedOut(w: PacketWriter, m: GroupDeactivatedOut) {
        w.int(m.int1)
    }
}
