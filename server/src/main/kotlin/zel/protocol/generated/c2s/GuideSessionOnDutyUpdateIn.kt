package zel.protocol.generated

data class GuideSessionOnDutyUpdateIn(
    val boolean1: Boolean,
    val boolean2: Boolean,
    val boolean3: Boolean,
    val boolean4: Boolean,
)

object GuideSessionOnDutyUpdateInCodec : IncomingCodec<GuideSessionOnDutyUpdateIn> {
    override val name = "GuideSessionOnDutyUpdateIn"

    override fun read(r: PacketReader): GuideSessionOnDutyUpdateIn = readGuideSessionOnDutyUpdateIn(r)

    override fun write(w: PacketWriter, m: GuideSessionOnDutyUpdateIn) = writeGuideSessionOnDutyUpdateIn(w, m)

    private fun readGuideSessionOnDutyUpdateIn(r: PacketReader): GuideSessionOnDutyUpdateIn {
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vBoolean3 = r.boolean()
        val vBoolean4 = r.boolean()
        return GuideSessionOnDutyUpdateIn(vBoolean1, vBoolean2, vBoolean3, vBoolean4)
    }

    private fun writeGuideSessionOnDutyUpdateIn(w: PacketWriter, m: GuideSessionOnDutyUpdateIn) {
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.boolean(m.boolean3)
        w.boolean(m.boolean4)
    }
}
