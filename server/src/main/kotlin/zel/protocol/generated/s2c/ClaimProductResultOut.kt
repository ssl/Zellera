package zel.protocol.generated

data class ClaimProductResultOut(
    val string1: String,
    val int1: Int,
)

object ClaimProductResultOutCodec : OutgoingCodec<ClaimProductResultOut> {
    override val name = "ClaimProductResultOut"

    override fun read(r: PacketReader): ClaimProductResultOut = readClaimProductResultOut(r)

    override fun write(w: PacketWriter, m: ClaimProductResultOut) = writeClaimProductResultOut(w, m)

    private fun readClaimProductResultOut(r: PacketReader): ClaimProductResultOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        return ClaimProductResultOut(vString1, vInt1)
    }

    private fun writeClaimProductResultOut(w: PacketWriter, m: ClaimProductResultOut) {
        w.string(m.string1)
        w.int(m.int1)
    }
}
