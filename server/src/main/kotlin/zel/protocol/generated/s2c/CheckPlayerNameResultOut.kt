package zel.protocol.generated

data class CheckPlayerNameResultOut(
    val int1: Int,
    val string1: String,
    val list1: List<String>,
)

object CheckPlayerNameResultOutCodec : OutgoingCodec<CheckPlayerNameResultOut> {
    override val name = "CheckPlayerNameResultOut"

    override fun read(r: PacketReader): CheckPlayerNameResultOut = readCheckPlayerNameResultOut(r)

    override fun write(w: PacketWriter, m: CheckPlayerNameResultOut) = writeCheckPlayerNameResultOut(w, m)

    private fun readCheckPlayerNameResultOut(r: PacketReader): CheckPlayerNameResultOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vList1 = List(r.countInt(2)) { r.string() }
        return CheckPlayerNameResultOut(vInt1, vString1, vList1)
    }

    private fun writeCheckPlayerNameResultOut(w: PacketWriter, m: CheckPlayerNameResultOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.countInt(m.list1.size)
        m.list1.forEach { w.string(it) }
    }
}
