package zel.protocol.generated

data class NftCollectiblesClaimBonusItemIn(
    val string1: String,
    val string2: String,
)

object NftCollectiblesClaimBonusItemInCodec : IncomingCodec<NftCollectiblesClaimBonusItemIn> {
    override val name = "NftCollectiblesClaimBonusItemIn"

    override fun read(r: PacketReader): NftCollectiblesClaimBonusItemIn = readNftCollectiblesClaimBonusItemIn(r)

    override fun write(w: PacketWriter, m: NftCollectiblesClaimBonusItemIn) = writeNftCollectiblesClaimBonusItemIn(w, m)

    private fun readNftCollectiblesClaimBonusItemIn(r: PacketReader): NftCollectiblesClaimBonusItemIn {
        val vString1 = r.string()
        val vString2 = r.string()
        return NftCollectiblesClaimBonusItemIn(vString1, vString2)
    }

    private fun writeNftCollectiblesClaimBonusItemIn(w: PacketWriter, m: NftCollectiblesClaimBonusItemIn) {
        w.string(m.string1)
        w.string(m.string2)
    }
}
