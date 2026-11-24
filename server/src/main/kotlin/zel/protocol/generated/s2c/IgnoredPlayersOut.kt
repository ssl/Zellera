package zel.protocol.generated

data class IgnoredPlayersOut(
    val list1: List<Int>,
)

object IgnoredPlayersOutCodec : OutgoingCodec<IgnoredPlayersOut> {
    override val name = "IgnoredPlayersOut"

    override fun read(r: PacketReader): IgnoredPlayersOut = readIgnoredPlayersOut(r)

    override fun write(w: PacketWriter, m: IgnoredPlayersOut) = writeIgnoredPlayersOut(w, m)

    private fun readIgnoredPlayersOut(r: PacketReader): IgnoredPlayersOut {
        val vList1 = List(r.countInt(4)) { r.int() }
        return IgnoredPlayersOut(vList1)
    }

    private fun writeIgnoredPlayersOut(w: PacketWriter, m: IgnoredPlayersOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
    }
}
