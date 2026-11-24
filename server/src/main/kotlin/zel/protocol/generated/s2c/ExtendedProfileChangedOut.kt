package zel.protocol.generated

data class ExtendedProfileChangedOut(
    val int1: Int,
)

object ExtendedProfileChangedOutCodec : OutgoingCodec<ExtendedProfileChangedOut> {
    override val name = "ExtendedProfileChangedOut"

    override fun read(r: PacketReader): ExtendedProfileChangedOut = readExtendedProfileChangedOut(r)

    override fun write(w: PacketWriter, m: ExtendedProfileChangedOut) = writeExtendedProfileChangedOut(w, m)

    private fun readExtendedProfileChangedOut(r: PacketReader): ExtendedProfileChangedOut {
        val vInt1 = r.int()
        return ExtendedProfileChangedOut(vInt1)
    }

    private fun writeExtendedProfileChangedOut(w: PacketWriter, m: ExtendedProfileChangedOut) {
        w.int(m.int1)
    }
}
