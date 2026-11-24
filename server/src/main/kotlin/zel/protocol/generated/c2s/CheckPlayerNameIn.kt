package zel.protocol.generated

data class CheckPlayerNameIn(
    val name: String,
)

object CheckPlayerNameInCodec : IncomingCodec<CheckPlayerNameIn> {
    override val name = "CheckPlayerNameIn"

    override fun read(r: PacketReader): CheckPlayerNameIn = readCheckPlayerNameIn(r)

    override fun write(w: PacketWriter, m: CheckPlayerNameIn) = writeCheckPlayerNameIn(w, m)

    private fun readCheckPlayerNameIn(r: PacketReader): CheckPlayerNameIn {
        val vName = r.string()
        return CheckPlayerNameIn(vName)
    }

    private fun writeCheckPlayerNameIn(w: PacketWriter, m: CheckPlayerNameIn) {
        w.string(m.name)
    }
}
