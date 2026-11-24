package zel.protocol.generated

data class NftBonusItemClaimResultOut(
    val string1: String,
    val string2: String,
    val boolean1: Boolean,
)

object NftBonusItemClaimResultOutCodec : OutgoingCodec<NftBonusItemClaimResultOut> {
    override val name = "NftBonusItemClaimResultOut"

    override fun read(r: PacketReader): NftBonusItemClaimResultOut = readNftBonusItemClaimResultOut(r)

    override fun write(w: PacketWriter, m: NftBonusItemClaimResultOut) = writeNftBonusItemClaimResultOut(w, m)

    private fun readNftBonusItemClaimResultOut(r: PacketReader): NftBonusItemClaimResultOut {
        val vString1 = r.string()
        val vString2 = r.string()
        val vBoolean1 = r.boolean()
        return NftBonusItemClaimResultOut(vString1, vString2, vBoolean1)
    }

    private fun writeNftBonusItemClaimResultOut(w: PacketWriter, m: NftBonusItemClaimResultOut) {
        w.string(m.string1)
        w.string(m.string2)
        w.boolean(m.boolean1)
    }
}
