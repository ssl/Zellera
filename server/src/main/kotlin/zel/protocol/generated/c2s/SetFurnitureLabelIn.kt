package zel.protocol.generated

data class SetFurnitureLabelIn(
    val furniId: Int,
    val text: String,
)

object SetFurnitureLabelInCodec : IncomingCodec<SetFurnitureLabelIn> {
    override val name = "SetFurnitureLabelIn"

    override fun read(r: PacketReader): SetFurnitureLabelIn = readSetFurnitureLabelIn(r)

    override fun write(w: PacketWriter, m: SetFurnitureLabelIn) = writeSetFurnitureLabelIn(w, m)

    private fun readSetFurnitureLabelIn(r: PacketReader): SetFurnitureLabelIn {
        val vFurniId = r.int()
        val vText = r.string()
        return SetFurnitureLabelIn(vFurniId, vText)
    }

    private fun writeSetFurnitureLabelIn(w: PacketWriter, m: SetFurnitureLabelIn) {
        w.int(m.furniId)
        w.string(m.text)
    }
}
