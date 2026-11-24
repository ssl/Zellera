package zel.protocol.generated

data class EpicPopupOut(
    val string1: String,
)

object EpicPopupOutCodec : OutgoingCodec<EpicPopupOut> {
    override val name = "EpicPopupOut"

    override fun read(r: PacketReader): EpicPopupOut = readEpicPopupOut(r)

    override fun write(w: PacketWriter, m: EpicPopupOut) = writeEpicPopupOut(w, m)

    private fun readEpicPopupOut(r: PacketReader): EpicPopupOut {
        val vString1 = r.string()
        return EpicPopupOut(vString1)
    }

    private fun writeEpicPopupOut(w: PacketWriter, m: EpicPopupOut) {
        w.string(m.string1)
    }
}
