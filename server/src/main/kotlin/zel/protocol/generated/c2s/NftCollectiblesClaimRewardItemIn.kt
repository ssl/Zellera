package zel.protocol.generated

data class NftCollectiblesClaimRewardItemIn(
    val string1: String,
    val string2: String,
)

object NftCollectiblesClaimRewardItemInCodec : IncomingCodec<NftCollectiblesClaimRewardItemIn> {
    override val name = "NftCollectiblesClaimRewardItemIn"

    override fun read(r: PacketReader): NftCollectiblesClaimRewardItemIn = readNftCollectiblesClaimRewardItemIn(r)

    override fun write(w: PacketWriter, m: NftCollectiblesClaimRewardItemIn) = writeNftCollectiblesClaimRewardItemIn(w, m)

    private fun readNftCollectiblesClaimRewardItemIn(r: PacketReader): NftCollectiblesClaimRewardItemIn {
        val vString1 = r.string()
        val vString2 = r.string()
        return NftCollectiblesClaimRewardItemIn(vString1, vString2)
    }

    private fun writeNftCollectiblesClaimRewardItemIn(w: PacketWriter, m: NftCollectiblesClaimRewardItemIn) {
        w.string(m.string1)
        w.string(m.string2)
    }
}
