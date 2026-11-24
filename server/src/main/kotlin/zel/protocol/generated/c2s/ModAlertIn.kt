package zel.protocol.generated

data class ModAlertIn(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val int3: Int,
)

object ModAlertInCodec : IncomingCodec<ModAlertIn> {
    override val name = "ModAlertIn"

    override fun read(r: PacketReader): ModAlertIn = readModAlertIn(r)

    override fun write(w: PacketWriter, m: ModAlertIn) = writeModAlertIn(w, m)

    private fun readModAlertIn(r: PacketReader): ModAlertIn {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return ModAlertIn(vInt1, vString1, vInt2, vInt3)
    }

    private fun writeModAlertIn(w: PacketWriter, m: ModAlertIn) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
