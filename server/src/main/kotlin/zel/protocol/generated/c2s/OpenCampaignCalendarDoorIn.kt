package zel.protocol.generated

data class OpenCampaignCalendarDoorIn(
    val campaignName: String,
    val day: Int,
)

object OpenCampaignCalendarDoorInCodec : IncomingCodec<OpenCampaignCalendarDoorIn> {
    override val name = "OpenCampaignCalendarDoorIn"

    override fun read(r: PacketReader): OpenCampaignCalendarDoorIn = readOpenCampaignCalendarDoorIn(r)

    override fun write(w: PacketWriter, m: OpenCampaignCalendarDoorIn) = writeOpenCampaignCalendarDoorIn(w, m)

    private fun readOpenCampaignCalendarDoorIn(r: PacketReader): OpenCampaignCalendarDoorIn {
        val vCampaignName = r.string()
        val vDay = r.int()
        return OpenCampaignCalendarDoorIn(vCampaignName, vDay)
    }

    private fun writeOpenCampaignCalendarDoorIn(w: PacketWriter, m: OpenCampaignCalendarDoorIn) {
        w.string(m.campaignName)
        w.int(m.day)
    }
}
