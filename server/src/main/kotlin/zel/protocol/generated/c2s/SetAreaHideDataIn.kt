package zel.protocol.generated

data class SetAreaHideDataIn(
    val itemId: Int,
    val rootX: Int,
    val rootY: Int,
    val width: Int,
    val length: Int,
    val invisibility: Boolean,
    val wallItems: Boolean,
    val invert: Boolean,
)

object SetAreaHideDataInCodec : IncomingCodec<SetAreaHideDataIn> {
    override val name = "SetAreaHideDataIn"

    override fun read(r: PacketReader): SetAreaHideDataIn = readSetAreaHideDataIn(r)

    override fun write(w: PacketWriter, m: SetAreaHideDataIn) = writeSetAreaHideDataIn(w, m)

    private fun readSetAreaHideDataIn(r: PacketReader): SetAreaHideDataIn {
        val vItemId = r.int()
        val vRootX = r.int()
        val vRootY = r.int()
        val vWidth = r.int()
        val vLength = r.int()
        val vInvisibility = r.boolean()
        val vWallItems = r.boolean()
        val vInvert = r.boolean()
        return SetAreaHideDataIn(vItemId, vRootX, vRootY, vWidth, vLength, vInvisibility, vWallItems, vInvert)
    }

    private fun writeSetAreaHideDataIn(w: PacketWriter, m: SetAreaHideDataIn) {
        w.int(m.itemId)
        w.int(m.rootX)
        w.int(m.rootY)
        w.int(m.width)
        w.int(m.length)
        w.boolean(m.invisibility)
        w.boolean(m.wallItems)
        w.boolean(m.invert)
    }
}
