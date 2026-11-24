package zel.protocol.generated

data class EmailStatusResultOut(
    val string1: String,
    val boolean1: Boolean,
    val boolean2: Boolean,
)

object EmailStatusResultOutCodec : OutgoingCodec<EmailStatusResultOut> {
    override val name = "EmailStatusResultOut"

    override fun read(r: PacketReader): EmailStatusResultOut = readEmailStatusResultOut(r)

    override fun write(w: PacketWriter, m: EmailStatusResultOut) = writeEmailStatusResultOut(w, m)

    private fun readEmailStatusResultOut(r: PacketReader): EmailStatusResultOut {
        val vString1 = r.string()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        return EmailStatusResultOut(vString1, vBoolean1, vBoolean2)
    }

    private fun writeEmailStatusResultOut(w: PacketWriter, m: EmailStatusResultOut) {
        w.string(m.string1)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
    }
}
