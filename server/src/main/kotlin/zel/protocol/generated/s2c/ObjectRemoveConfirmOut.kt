package zel.protocol.generated

data class ObjectRemoveConfirmOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val string2: String,
)

object ObjectRemoveConfirmOutCodec : OutgoingCodec<ObjectRemoveConfirmOut> {
    override val name = "ObjectRemoveConfirmOut"

    override fun read(r: PacketReader): ObjectRemoveConfirmOut = readObjectRemoveConfirmOut(r)

    override fun write(w: PacketWriter, m: ObjectRemoveConfirmOut) = writeObjectRemoveConfirmOut(w, m)

    private fun readObjectRemoveConfirmOut(r: PacketReader): ObjectRemoveConfirmOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        return ObjectRemoveConfirmOut(vInt1, vInt2, vString1, vString2)
    }

    private fun writeObjectRemoveConfirmOut(w: PacketWriter, m: ObjectRemoveConfirmOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.string(m.string2)
    }
}
