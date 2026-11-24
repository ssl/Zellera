package zel.protocol.generated

data class NftClaimResultOut(
    val short1: Short,
)

object NftClaimResultOutCodec : OutgoingCodec<NftClaimResultOut> {
    override val name = "NftClaimResultOut"

    override fun read(r: PacketReader): NftClaimResultOut = readNftClaimResultOut(r)

    override fun write(w: PacketWriter, m: NftClaimResultOut) = writeNftClaimResultOut(w, m)

    private fun readNftClaimResultOut(r: PacketReader): NftClaimResultOut {
        val vShort1 = r.short()
        return NftClaimResultOut(vShort1)
    }

    private fun writeNftClaimResultOut(w: PacketWriter, m: NftClaimResultOut) {
        w.short(m.short1)
    }
}
