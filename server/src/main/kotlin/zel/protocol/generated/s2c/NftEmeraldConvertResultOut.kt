package zel.protocol.generated

data class NftEmeraldConvertResultOut(
    val int1: Int,
    val short1: Short,
)

object NftEmeraldConvertResultOutCodec : OutgoingCodec<NftEmeraldConvertResultOut> {
    override val name = "NftEmeraldConvertResultOut"

    override fun read(r: PacketReader): NftEmeraldConvertResultOut = readNftEmeraldConvertResultOut(r)

    override fun write(w: PacketWriter, m: NftEmeraldConvertResultOut) = writeNftEmeraldConvertResultOut(w, m)

    private fun readNftEmeraldConvertResultOut(r: PacketReader): NftEmeraldConvertResultOut {
        val vInt1 = r.int()
        val vShort1 = r.short()
        return NftEmeraldConvertResultOut(vInt1, vShort1)
    }

    private fun writeNftEmeraldConvertResultOut(w: PacketWriter, m: NftEmeraldConvertResultOut) {
        w.int(m.int1)
        w.short(m.short1)
    }
}
