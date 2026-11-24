package zel.protocol.generated

data class IsFirstLoginOfDayOut(
    val boolean1: Boolean,
)

object IsFirstLoginOfDayOutCodec : OutgoingCodec<IsFirstLoginOfDayOut> {
    override val name = "IsFirstLoginOfDayOut"

    override fun read(r: PacketReader): IsFirstLoginOfDayOut = readIsFirstLoginOfDayOut(r)

    override fun write(w: PacketWriter, m: IsFirstLoginOfDayOut) = writeIsFirstLoginOfDayOut(w, m)

    private fun readIsFirstLoginOfDayOut(r: PacketReader): IsFirstLoginOfDayOut {
        val vBoolean1 = r.boolean()
        return IsFirstLoginOfDayOut(vBoolean1)
    }

    private fun writeIsFirstLoginOfDayOut(w: PacketWriter, m: IsFirstLoginOfDayOut) {
        w.boolean(m.boolean1)
    }
}
