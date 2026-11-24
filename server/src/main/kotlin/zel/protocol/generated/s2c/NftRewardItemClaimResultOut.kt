package zel.protocol.generated

data class NftRewardItemClaimResultOut(
    val string1: String,
    val string2: String,
    val boolean1: Boolean,
)

object NftRewardItemClaimResultOutCodec : OutgoingCodec<NftRewardItemClaimResultOut> {
    override val name = "NftRewardItemClaimResultOut"

    override fun read(r: PacketReader): NftRewardItemClaimResultOut = readNftRewardItemClaimResultOut(r)

    override fun write(w: PacketWriter, m: NftRewardItemClaimResultOut) = writeNftRewardItemClaimResultOut(w, m)

    private fun readNftRewardItemClaimResultOut(r: PacketReader): NftRewardItemClaimResultOut {
        val vString1 = r.string()
        val vString2 = r.string()
        val vBoolean1 = r.boolean()
        return NftRewardItemClaimResultOut(vString1, vString2, vBoolean1)
    }

    private fun writeNftRewardItemClaimResultOut(w: PacketWriter, m: NftRewardItemClaimResultOut) {
        w.string(m.string1)
        w.string(m.string2)
        w.boolean(m.boolean1)
    }
}
