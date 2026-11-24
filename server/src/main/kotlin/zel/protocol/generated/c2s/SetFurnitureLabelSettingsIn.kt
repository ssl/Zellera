package zel.protocol.generated

data class SetFurnitureLabelSettingsIn(
    val furniId: Int,
    val int1: Int,
    val int2: Int,
    val flag: Boolean,
)

object SetFurnitureLabelSettingsInCodec : IncomingCodec<SetFurnitureLabelSettingsIn> {
    override val name = "SetFurnitureLabelSettingsIn"

    override fun read(r: PacketReader): SetFurnitureLabelSettingsIn = readSetFurnitureLabelSettingsIn(r)

    override fun write(w: PacketWriter, m: SetFurnitureLabelSettingsIn) = writeSetFurnitureLabelSettingsIn(w, m)

    private fun readSetFurnitureLabelSettingsIn(r: PacketReader): SetFurnitureLabelSettingsIn {
        val vFurniId = r.int()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vFlag = r.boolean()
        return SetFurnitureLabelSettingsIn(vFurniId, vInt1, vInt2, vFlag)
    }

    private fun writeSetFurnitureLabelSettingsIn(w: PacketWriter, m: SetFurnitureLabelSettingsIn) {
        w.int(m.furniId)
        w.int(m.int1)
        w.int(m.int2)
        w.boolean(m.flag)
    }
}
