package zel.protocol.generated

data class MintItemIn(
    val int1: Int,
    val string1: String,
)

object MintItemInCodec : IncomingCodec<MintItemIn> {
    override val name = "MintItemIn"

    override fun read(r: PacketReader): MintItemIn = readMintItemIn(r)

    override fun write(w: PacketWriter, m: MintItemIn) = writeMintItemIn(w, m)

    private fun readMintItemIn(r: PacketReader): MintItemIn {
        val vInt1 = r.int()
        val vString1 = r.string()
        return MintItemIn(vInt1, vString1)
    }

    private fun writeMintItemIn(w: PacketWriter, m: MintItemIn) {
        w.int(m.int1)
        w.string(m.string1)
    }
}
