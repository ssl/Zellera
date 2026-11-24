package zel.protocol.generated

data class ChangeEmailIn(
    val string1: String,
)

object ChangeEmailInCodec : IncomingCodec<ChangeEmailIn> {
    override val name = "ChangeEmailIn"

    override fun read(r: PacketReader): ChangeEmailIn = readChangeEmailIn(r)

    override fun write(w: PacketWriter, m: ChangeEmailIn) = writeChangeEmailIn(w, m)

    private fun readChangeEmailIn(r: PacketReader): ChangeEmailIn {
        val vString1 = r.string()
        return ChangeEmailIn(vString1)
    }

    private fun writeChangeEmailIn(w: PacketWriter, m: ChangeEmailIn) {
        w.string(m.string1)
    }
}
