package zel.protocol.generated

data class ElementPointerOut(
    val string1: String,
)

object ElementPointerOutCodec : OutgoingCodec<ElementPointerOut> {
    override val name = "ElementPointerOut"

    override fun read(r: PacketReader): ElementPointerOut = readElementPointerOut(r)

    override fun write(w: PacketWriter, m: ElementPointerOut) = writeElementPointerOut(w, m)

    private fun readElementPointerOut(r: PacketReader): ElementPointerOut {
        val vString1 = r.string()
        return ElementPointerOut(vString1)
    }

    private fun writeElementPointerOut(w: PacketWriter, m: ElementPointerOut) {
        w.string(m.string1)
    }
}
