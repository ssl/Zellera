package zel.protocol.generated

data class VariableLeaderboardSubscribeIn(
    val int1: Int,
    val int2: Int,
    val key: String,
    val ids: List<Int>,
)

object VariableLeaderboardSubscribeInCodec : IncomingCodec<VariableLeaderboardSubscribeIn> {
    override val name = "VariableLeaderboardSubscribeIn"

    override fun read(r: PacketReader): VariableLeaderboardSubscribeIn = readVariableLeaderboardSubscribeIn(r)

    override fun write(w: PacketWriter, m: VariableLeaderboardSubscribeIn) = writeVariableLeaderboardSubscribeIn(w, m)

    private fun readVariableLeaderboardSubscribeIn(r: PacketReader): VariableLeaderboardSubscribeIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vKey = r.string()
        val vIds = List(r.countInt(4)) { r.int() }
        return VariableLeaderboardSubscribeIn(vInt1, vInt2, vKey, vIds)
    }

    private fun writeVariableLeaderboardSubscribeIn(w: PacketWriter, m: VariableLeaderboardSubscribeIn) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.key)
        w.countInt(m.ids.size)
        m.ids.forEach { w.int(it) }
    }
}
