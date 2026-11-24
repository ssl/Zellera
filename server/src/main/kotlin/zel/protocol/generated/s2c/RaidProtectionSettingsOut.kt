package zel.protocol.generated

data class RaidProtectionSettingsOut(
    val int1: Int,
    val boolean1: Boolean,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val boolean2: Boolean,
    val int5: Int,
    val int6: Int,
    val boolean3: Boolean,
    val int7: Int,
)

object RaidProtectionSettingsOutCodec : OutgoingCodec<RaidProtectionSettingsOut> {
    override val name = "RaidProtectionSettingsOut"

    override fun read(r: PacketReader): RaidProtectionSettingsOut = readRaidProtectionSettingsOut(r)

    override fun write(w: PacketWriter, m: RaidProtectionSettingsOut) = writeRaidProtectionSettingsOut(w, m)

    private fun readRaidProtectionSettingsOut(r: PacketReader): RaidProtectionSettingsOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vBoolean2 = r.boolean()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vBoolean3 = r.boolean()
        val vInt7 = r.int()
        return RaidProtectionSettingsOut(vInt1, vBoolean1, vInt2, vInt3, vInt4, vBoolean2, vInt5, vInt6, vBoolean3, vInt7)
    }

    private fun writeRaidProtectionSettingsOut(w: PacketWriter, m: RaidProtectionSettingsOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.boolean(m.boolean2)
        w.int(m.int5)
        w.int(m.int6)
        w.boolean(m.boolean3)
        w.int(m.int7)
    }
}
