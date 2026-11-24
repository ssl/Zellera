package zel.protocol.generated

data class SetMannequinNameIn(
    val itemId: Int,
    val name: String,
)

object SetMannequinNameInCodec : IncomingCodec<SetMannequinNameIn> {
    override val name = "SetMannequinNameIn"

    override fun read(r: PacketReader): SetMannequinNameIn = readSetMannequinNameIn(r)

    override fun write(w: PacketWriter, m: SetMannequinNameIn) = writeSetMannequinNameIn(w, m)

    private fun readSetMannequinNameIn(r: PacketReader): SetMannequinNameIn {
        val vItemId = r.int()
        val vName = r.string()
        return SetMannequinNameIn(vItemId, vName)
    }

    private fun writeSetMannequinNameIn(w: PacketWriter, m: SetMannequinNameIn) {
        w.int(m.itemId)
        w.string(m.name)
    }
}
