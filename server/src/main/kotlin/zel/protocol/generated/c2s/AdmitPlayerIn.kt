package zel.protocol.generated

data class AdmitPlayerIn(
    val string1: String,
    val boolean1: Boolean,
)

object AdmitPlayerInCodec : IncomingCodec<AdmitPlayerIn> {
    override val name = "AdmitPlayerIn"

    override fun read(r: PacketReader): AdmitPlayerIn = readAdmitPlayerIn(r)

    override fun write(w: PacketWriter, m: AdmitPlayerIn) = writeAdmitPlayerIn(w, m)

    private fun readAdmitPlayerIn(r: PacketReader): AdmitPlayerIn {
        val vString1 = r.string()
        val vBoolean1 = r.boolean()
        return AdmitPlayerIn(vString1, vBoolean1)
    }

    private fun writeAdmitPlayerIn(w: PacketWriter, m: AdmitPlayerIn) {
        w.string(m.string1)
        w.boolean(m.boolean1)
    }
}
