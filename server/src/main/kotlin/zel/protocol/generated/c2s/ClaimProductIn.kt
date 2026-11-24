package zel.protocol.generated

data class ClaimProductIn(
    val claimId: String,
    val choice: Int?,
)

object ClaimProductInCodec : IncomingCodec<ClaimProductIn> {
    override val name = "ClaimProductIn"

    override fun read(r: PacketReader): ClaimProductIn = readClaimProductIn(r)

    override fun write(w: PacketWriter, m: ClaimProductIn) = writeClaimProductIn(w, m)

    private fun readClaimProductIn(r: PacketReader): ClaimProductIn {
        val vClaimId = r.string()
        val vChoice = if (r.remaining > 0) r.int() else null
        return ClaimProductIn(vClaimId, vChoice)
    }

    private fun writeClaimProductIn(w: PacketWriter, m: ClaimProductIn) {
        w.string(m.claimId)
        m.choice?.let { w.int(it) }
    }
}
