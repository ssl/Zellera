package zel.protocol.generated

data class ModMuteIn(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val trailing1: Int?,
)

object ModMuteInCodec : IncomingCodec<ModMuteIn> {
    override val name = "ModMuteIn"

    override fun read(r: PacketReader): ModMuteIn = readModMuteIn(r)

    override fun write(w: PacketWriter, m: ModMuteIn) = writeModMuteIn(w, m)

    private fun readModMuteIn(r: PacketReader): ModMuteIn {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vTrailing1 = if (r.remaining > 0) r.int() else null
        return ModMuteIn(vInt1, vString1, vInt2, vTrailing1)
    }

    private fun writeModMuteIn(w: PacketWriter, m: ModMuteIn) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        m.trailing1?.let { w.int(it) }
    }
}
