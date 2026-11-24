package zel.protocol.generated

data class BuildPassSubscriptionStatusOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val trailing1: Int?,
)

object BuildPassSubscriptionStatusOutCodec : OutgoingCodec<BuildPassSubscriptionStatusOut> {
    override val name = "BuildPassSubscriptionStatusOut"

    override fun read(r: PacketReader): BuildPassSubscriptionStatusOut = readBuildPassSubscriptionStatusOut(r)

    override fun write(w: PacketWriter, m: BuildPassSubscriptionStatusOut) = writeBuildPassSubscriptionStatusOut(w, m)

    private fun readBuildPassSubscriptionStatusOut(r: PacketReader): BuildPassSubscriptionStatusOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vTrailing1 = if (r.remaining > 0) r.int() else null
        return BuildPassSubscriptionStatusOut(vInt1, vInt2, vInt3, vTrailing1)
    }

    private fun writeBuildPassSubscriptionStatusOut(w: PacketWriter, m: BuildPassSubscriptionStatusOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        m.trailing1?.let { w.int(it) }
    }
}
