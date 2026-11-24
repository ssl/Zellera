package zel.protocol.generated

data class HasClaimedProductIn(
    val claimId: String,
)

object HasClaimedProductInCodec : IncomingCodec<HasClaimedProductIn> {
    override val name = "HasClaimedProductIn"

    override fun read(r: PacketReader): HasClaimedProductIn = readHasClaimedProductIn(r)

    override fun write(w: PacketWriter, m: HasClaimedProductIn) = writeHasClaimedProductIn(w, m)

    private fun readHasClaimedProductIn(r: PacketReader): HasClaimedProductIn {
        val vClaimId = r.string()
        return HasClaimedProductIn(vClaimId)
    }

    private fun writeHasClaimedProductIn(w: PacketWriter, m: HasClaimedProductIn) {
        w.string(m.claimId)
    }
}
