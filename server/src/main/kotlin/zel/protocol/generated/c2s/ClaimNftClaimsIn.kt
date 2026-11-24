package zel.protocol.generated

data class ClaimNftClaimsIn(
    val string1: String,
    val string2: String,
)

object ClaimNftClaimsInCodec : IncomingCodec<ClaimNftClaimsIn> {
    override val name = "ClaimNftClaimsIn"

    override fun read(r: PacketReader): ClaimNftClaimsIn = readClaimNftClaimsIn(r)

    override fun write(w: PacketWriter, m: ClaimNftClaimsIn) = writeClaimNftClaimsIn(w, m)

    private fun readClaimNftClaimsIn(r: PacketReader): ClaimNftClaimsIn {
        val vString1 = r.string()
        val vString2 = r.string()
        return ClaimNftClaimsIn(vString1, vString2)
    }

    private fun writeClaimNftClaimsIn(w: PacketWriter, m: ClaimNftClaimsIn) {
        w.string(m.string1)
        w.string(m.string2)
    }
}
