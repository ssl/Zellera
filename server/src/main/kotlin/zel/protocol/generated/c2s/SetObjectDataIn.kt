package zel.protocol.generated

data class SetObjectDataIn(
    val itemId: Int,
    val keysAndValues: List<String>,
)

object SetObjectDataInCodec : IncomingCodec<SetObjectDataIn> {
    override val name = "SetObjectDataIn"

    override fun read(r: PacketReader): SetObjectDataIn = readSetObjectDataIn(r)

    override fun write(w: PacketWriter, m: SetObjectDataIn) = writeSetObjectDataIn(w, m)

    private fun readSetObjectDataIn(r: PacketReader): SetObjectDataIn {
        val vItemId = r.int()
        val vKeysAndValues = List(r.countInt(2)) { r.string() }
        return SetObjectDataIn(vItemId, vKeysAndValues)
    }

    private fun writeSetObjectDataIn(w: PacketWriter, m: SetObjectDataIn) {
        w.int(m.itemId)
        w.countInt(m.keysAndValues.size)
        m.keysAndValues.forEach { w.string(it) }
    }
}
