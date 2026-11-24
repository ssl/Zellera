package zel.protocol.generated

data class RaidProtectionSettingsResultOut(
    val int1: Int,
    val int2: Int,
    val boolean1: Boolean,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val boolean2: Boolean,
    val int6: Int,
    val int7: Int,
    val boolean3: Boolean,
    val int8: Int,
)

object RaidProtectionSettingsResultOutCodec : OutgoingCodec<RaidProtectionSettingsResultOut> {
    override val name = "RaidProtectionSettingsResultOut"

    override fun read(r: PacketReader): RaidProtectionSettingsResultOut = readRaidProtectionSettingsResultOut(r)

    override fun write(w: PacketWriter, m: RaidProtectionSettingsResultOut) = writeRaidProtectionSettingsResultOut(w, m)

    private fun readRaidProtectionSettingsResultOut(r: PacketReader): RaidProtectionSettingsResultOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vBoolean2 = r.boolean()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vBoolean3 = r.boolean()
        val vInt8 = r.int()
        return RaidProtectionSettingsResultOut(vInt1, vInt2, vBoolean1, vInt3, vInt4, vInt5, vBoolean2, vInt6, vInt7, vBoolean3, vInt8)
    }

    private fun writeRaidProtectionSettingsResultOut(w: PacketWriter, m: RaidProtectionSettingsResultOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.boolean(m.boolean1)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.boolean(m.boolean2)
        w.int(m.int6)
        w.int(m.int7)
        w.boolean(m.boolean3)
        w.int(m.int8)
    }
}
