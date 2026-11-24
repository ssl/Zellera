package zel.protocol.generated

data class RaidProtectionCapabilityOut(
    val int1: Int,
    val boolean1: Boolean,
)

object RaidProtectionCapabilityOutCodec : OutgoingCodec<RaidProtectionCapabilityOut> {
    override val name = "RaidProtectionCapabilityOut"

    override fun read(r: PacketReader): RaidProtectionCapabilityOut = readRaidProtectionCapabilityOut(r)

    override fun write(w: PacketWriter, m: RaidProtectionCapabilityOut) = writeRaidProtectionCapabilityOut(w, m)

    private fun readRaidProtectionCapabilityOut(r: PacketReader): RaidProtectionCapabilityOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        return RaidProtectionCapabilityOut(vInt1, vBoolean1)
    }

    private fun writeRaidProtectionCapabilityOut(w: PacketWriter, m: RaidProtectionCapabilityOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
    }
}
