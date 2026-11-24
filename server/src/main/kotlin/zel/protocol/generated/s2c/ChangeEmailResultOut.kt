package zel.protocol.generated

data class ChangeEmailResultOut(
    val int1: Int,
)

object ChangeEmailResultOutCodec : OutgoingCodec<ChangeEmailResultOut> {
    override val name = "ChangeEmailResultOut"

    override fun read(r: PacketReader): ChangeEmailResultOut = readChangeEmailResultOut(r)

    override fun write(w: PacketWriter, m: ChangeEmailResultOut) = writeChangeEmailResultOut(w, m)

    private fun readChangeEmailResultOut(r: PacketReader): ChangeEmailResultOut {
        val vInt1 = r.int()
        return ChangeEmailResultOut(vInt1)
    }

    private fun writeChangeEmailResultOut(w: PacketWriter, m: ChangeEmailResultOut) {
        w.int(m.int1)
    }
}
