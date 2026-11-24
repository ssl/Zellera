package zel.protocol.generated

data class ChangeMottoIn(
    val motto: String,
)

object ChangeMottoInCodec : IncomingCodec<ChangeMottoIn> {
    override val name = "ChangeMottoIn"

    override fun read(r: PacketReader): ChangeMottoIn = readChangeMottoIn(r)

    override fun write(w: PacketWriter, m: ChangeMottoIn) = writeChangeMottoIn(w, m)

    private fun readChangeMottoIn(r: PacketReader): ChangeMottoIn {
        val vMotto = r.string()
        return ChangeMottoIn(vMotto)
    }

    private fun writeChangeMottoIn(w: PacketWriter, m: ChangeMottoIn) {
        w.string(m.motto)
    }
}
