package zel.protocol.generated

data class ObjectDataUpdateOut(
    val string1: String,
    val stuff1: StuffData,
)

object ObjectDataUpdateOutCodec : OutgoingCodec<ObjectDataUpdateOut> {
    override val name = "ObjectDataUpdateOut"

    override fun read(r: PacketReader): ObjectDataUpdateOut = readObjectDataUpdateOut(r)

    override fun write(w: PacketWriter, m: ObjectDataUpdateOut) = writeObjectDataUpdateOut(w, m)

    private fun readObjectDataUpdateOut(r: PacketReader): ObjectDataUpdateOut {
        val vString1 = r.string()
        val vStuff1 = StuffData.read(r)
        return ObjectDataUpdateOut(vString1, vStuff1)
    }

    private fun writeObjectDataUpdateOut(w: PacketWriter, m: ObjectDataUpdateOut) {
        w.string(m.string1)
        StuffData.write(w, m.stuff1)
    }
}
