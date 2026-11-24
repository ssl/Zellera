package zel.protocol.generated

data class ObjectRemoveOut(
    val string1: String,
    val boolean1: Boolean,
    val int1: Int,
    val int2: Int,
    val boolean2: Boolean,
)

object ObjectRemoveOutCodec : OutgoingCodec<ObjectRemoveOut> {
    override val name = "ObjectRemoveOut"

    override fun read(r: PacketReader): ObjectRemoveOut = readObjectRemoveOut(r)

    override fun write(w: PacketWriter, m: ObjectRemoveOut) = writeObjectRemoveOut(w, m)

    private fun readObjectRemoveOut(r: PacketReader): ObjectRemoveOut {
        val vString1 = r.string()
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vBoolean2 = r.boolean()
        return ObjectRemoveOut(vString1, vBoolean1, vInt1, vInt2, vBoolean2)
    }

    private fun writeObjectRemoveOut(w: PacketWriter, m: ObjectRemoveOut) {
        w.string(m.string1)
        w.boolean(m.boolean1)
        w.int(m.int1)
        w.int(m.int2)
        w.boolean(m.boolean2)
    }
}
