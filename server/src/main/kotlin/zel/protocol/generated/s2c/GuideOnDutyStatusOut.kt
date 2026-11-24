package zel.protocol.generated

data class GuideOnDutyStatusOut(
    val boolean1: Boolean,
    val int1: Int,
    val int2: Int,
    val int3: Int,
)

object GuideOnDutyStatusOutCodec : OutgoingCodec<GuideOnDutyStatusOut> {
    override val name = "GuideOnDutyStatusOut"

    override fun read(r: PacketReader): GuideOnDutyStatusOut = readGuideOnDutyStatusOut(r)

    override fun write(w: PacketWriter, m: GuideOnDutyStatusOut) = writeGuideOnDutyStatusOut(w, m)

    private fun readGuideOnDutyStatusOut(r: PacketReader): GuideOnDutyStatusOut {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return GuideOnDutyStatusOut(vBoolean1, vInt1, vInt2, vInt3)
    }

    private fun writeGuideOnDutyStatusOut(w: PacketWriter, m: GuideOnDutyStatusOut) {
        w.boolean(m.boolean1)
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
