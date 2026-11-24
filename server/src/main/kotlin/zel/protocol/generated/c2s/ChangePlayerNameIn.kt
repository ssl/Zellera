package zel.protocol.generated

data class ChangePlayerNameIn(
    val name: String,
)

object ChangePlayerNameInCodec : IncomingCodec<ChangePlayerNameIn> {
    override val name = "ChangePlayerNameIn"

    override fun read(r: PacketReader): ChangePlayerNameIn = readChangePlayerNameIn(r)

    override fun write(w: PacketWriter, m: ChangePlayerNameIn) = writeChangePlayerNameIn(w, m)

    private fun readChangePlayerNameIn(r: PacketReader): ChangePlayerNameIn {
        val vName = r.string()
        return ChangePlayerNameIn(vName)
    }

    private fun writeChangePlayerNameIn(w: PacketWriter, m: ChangePlayerNameIn) {
        w.string(m.name)
    }
}
