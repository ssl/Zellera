package zel.protocol.generated

data class ClaimDailyTaskIn(
    val wireId: Int,
)

object ClaimDailyTaskInCodec : IncomingCodec<ClaimDailyTaskIn> {
    override val name = "ClaimDailyTaskIn"

    override fun read(r: PacketReader): ClaimDailyTaskIn = readClaimDailyTaskIn(r)

    override fun write(w: PacketWriter, m: ClaimDailyTaskIn) = writeClaimDailyTaskIn(w, m)

    private fun readClaimDailyTaskIn(r: PacketReader): ClaimDailyTaskIn {
        val vWireId = r.int()
        return ClaimDailyTaskIn(vWireId)
    }

    private fun writeClaimDailyTaskIn(w: PacketWriter, m: ClaimDailyTaskIn) {
        w.int(m.wireId)
    }
}
