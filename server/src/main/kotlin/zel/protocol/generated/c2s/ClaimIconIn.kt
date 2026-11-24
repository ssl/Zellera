package zel.protocol.generated

data class ClaimIconIn(
    val id: Int,
)

object ClaimIconInCodec : IncomingCodec<ClaimIconIn> {
    override val name = "ClaimIconIn"

    override fun read(r: PacketReader): ClaimIconIn = readClaimIconIn(r)

    override fun write(w: PacketWriter, m: ClaimIconIn) = writeClaimIconIn(w, m)

    private fun readClaimIconIn(r: PacketReader): ClaimIconIn {
        val vId = r.int()
        return ClaimIconIn(vId)
    }

    private fun writeClaimIconIn(w: PacketWriter, m: ClaimIconIn) {
        w.int(m.id)
    }
}
