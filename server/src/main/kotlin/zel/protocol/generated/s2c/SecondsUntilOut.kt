package zel.protocol.generated

data class SecondsUntilOut(
    val date: String,
    val seconds: Int,
)

object SecondsUntilOutCodec : OutgoingCodec<SecondsUntilOut> {
    override val name = "SecondsUntilOut"

    override fun read(r: PacketReader): SecondsUntilOut = readSecondsUntilOut(r)

    override fun write(w: PacketWriter, m: SecondsUntilOut) = writeSecondsUntilOut(w, m)

    private fun readSecondsUntilOut(r: PacketReader): SecondsUntilOut {
        val vDate = r.string()
        val vSeconds = r.int()
        return SecondsUntilOut(vDate, vSeconds)
    }

    private fun writeSecondsUntilOut(w: PacketWriter, m: SecondsUntilOut) {
        w.string(m.date)
        w.int(m.seconds)
    }
}
