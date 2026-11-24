package zel.protocol.generated

data class ModTradingLockIn(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val int3: Int,
    val trailing1: Int?,
)

object ModTradingLockInCodec : IncomingCodec<ModTradingLockIn> {
    override val name = "ModTradingLockIn"

    override fun read(r: PacketReader): ModTradingLockIn = readModTradingLockIn(r)

    override fun write(w: PacketWriter, m: ModTradingLockIn) = writeModTradingLockIn(w, m)

    private fun readModTradingLockIn(r: PacketReader): ModTradingLockIn {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vTrailing1 = if (r.remaining > 0) r.int() else null
        return ModTradingLockIn(vInt1, vString1, vInt2, vInt3, vTrailing1)
    }

    private fun writeModTradingLockIn(w: PacketWriter, m: ModTradingLockIn) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
        m.trailing1?.let { w.int(it) }
    }
}
