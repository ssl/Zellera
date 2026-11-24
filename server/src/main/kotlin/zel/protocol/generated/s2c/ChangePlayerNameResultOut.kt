package zel.protocol.generated

data class ChangePlayerNameResultOut(
    val int1: Int,
    val string1: String,
    val list1: List<String>,
)

object ChangePlayerNameResultOutCodec : OutgoingCodec<ChangePlayerNameResultOut> {
    override val name = "ChangePlayerNameResultOut"

    override fun read(r: PacketReader): ChangePlayerNameResultOut = readChangePlayerNameResultOut(r)

    override fun write(w: PacketWriter, m: ChangePlayerNameResultOut) = writeChangePlayerNameResultOut(w, m)

    private fun readChangePlayerNameResultOut(r: PacketReader): ChangePlayerNameResultOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vList1 = List(r.countInt(2)) { r.string() }
        return ChangePlayerNameResultOut(vInt1, vString1, vList1)
    }

    private fun writeChangePlayerNameResultOut(w: PacketWriter, m: ChangePlayerNameResultOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.countInt(m.list1.size)
        m.list1.forEach { w.string(it) }
    }
}
