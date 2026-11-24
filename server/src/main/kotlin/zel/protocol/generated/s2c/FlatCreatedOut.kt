package zel.protocol.generated

data class FlatCreatedOut(
    val flatId: Int,
    val flatName: String,
)

object FlatCreatedOutCodec : OutgoingCodec<FlatCreatedOut> {
    override val name = "FlatCreatedOut"

    override fun read(r: PacketReader): FlatCreatedOut = readFlatCreatedOut(r)

    override fun write(w: PacketWriter, m: FlatCreatedOut) = writeFlatCreatedOut(w, m)

    private fun readFlatCreatedOut(r: PacketReader): FlatCreatedOut {
        val vFlatId = r.int()
        val vFlatName = r.string()
        return FlatCreatedOut(vFlatId, vFlatName)
    }

    private fun writeFlatCreatedOut(w: PacketWriter, m: FlatCreatedOut) {
        w.int(m.flatId)
        w.string(m.flatName)
    }
}
