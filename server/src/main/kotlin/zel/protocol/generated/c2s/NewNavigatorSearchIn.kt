package zel.protocol.generated

data class NewNavigatorSearchIn(
    val view: String,
    val query: String,
)

object NewNavigatorSearchInCodec : IncomingCodec<NewNavigatorSearchIn> {
    override val name = "NewNavigatorSearchIn"

    override fun read(r: PacketReader): NewNavigatorSearchIn = readNewNavigatorSearchIn(r)

    override fun write(w: PacketWriter, m: NewNavigatorSearchIn) = writeNewNavigatorSearchIn(w, m)

    private fun readNewNavigatorSearchIn(r: PacketReader): NewNavigatorSearchIn {
        val vView = r.string()
        val vQuery = r.string()
        return NewNavigatorSearchIn(vView, vQuery)
    }

    private fun writeNewNavigatorSearchIn(w: PacketWriter, m: NewNavigatorSearchIn) {
        w.string(m.view)
        w.string(m.query)
    }
}
