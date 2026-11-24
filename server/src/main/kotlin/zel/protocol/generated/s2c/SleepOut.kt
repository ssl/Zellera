package zel.protocol.generated

data class SleepOut(
    val int1: Int,
    val boolean1: Boolean,
)

object SleepOutCodec : OutgoingCodec<SleepOut> {
    override val name = "SleepOut"

    override fun read(r: PacketReader): SleepOut = readSleepOut(r)

    override fun write(w: PacketWriter, m: SleepOut) = writeSleepOut(w, m)

    private fun readSleepOut(r: PacketReader): SleepOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        return SleepOut(vInt1, vBoolean1)
    }

    private fun writeSleepOut(w: PacketWriter, m: SleepOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
    }
}
