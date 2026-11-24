package zel.protocol.generated

data class SetClothingChangeDataIn(
    val itemId: Int,
    val gender: String,
    val look: String,
)

object SetClothingChangeDataInCodec : IncomingCodec<SetClothingChangeDataIn> {
    override val name = "SetClothingChangeDataIn"

    override fun read(r: PacketReader): SetClothingChangeDataIn = readSetClothingChangeDataIn(r)

    override fun write(w: PacketWriter, m: SetClothingChangeDataIn) = writeSetClothingChangeDataIn(w, m)

    private fun readSetClothingChangeDataIn(r: PacketReader): SetClothingChangeDataIn {
        val vItemId = r.int()
        val vGender = r.string()
        val vLook = r.string()
        return SetClothingChangeDataIn(vItemId, vGender, vLook)
    }

    private fun writeSetClothingChangeDataIn(w: PacketWriter, m: SetClothingChangeDataIn) {
        w.int(m.itemId)
        w.string(m.gender)
        w.string(m.look)
    }
}
