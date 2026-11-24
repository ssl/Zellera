package zel.protocol.generated

data class CampaignCalendarDoorOpenedOut(
    val boolean1: Boolean,
    val string1: String,
    val string2: String,
    val string3: String,
)

object CampaignCalendarDoorOpenedOutCodec : OutgoingCodec<CampaignCalendarDoorOpenedOut> {
    override val name = "CampaignCalendarDoorOpenedOut"

    override fun read(r: PacketReader): CampaignCalendarDoorOpenedOut = readCampaignCalendarDoorOpenedOut(r)

    override fun write(w: PacketWriter, m: CampaignCalendarDoorOpenedOut) = writeCampaignCalendarDoorOpenedOut(w, m)

    private fun readCampaignCalendarDoorOpenedOut(r: PacketReader): CampaignCalendarDoorOpenedOut {
        val vBoolean1 = r.boolean()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        return CampaignCalendarDoorOpenedOut(vBoolean1, vString1, vString2, vString3)
    }

    private fun writeCampaignCalendarDoorOpenedOut(w: PacketWriter, m: CampaignCalendarDoorOpenedOut) {
        w.boolean(m.boolean1)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
    }
}
