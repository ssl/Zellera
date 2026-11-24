package zel.protocol.generated

data class OpenCampaignCalendarDoorAsStaffIn(
    val campaignName: String,
    val day: Int,
)

object OpenCampaignCalendarDoorAsStaffInCodec : IncomingCodec<OpenCampaignCalendarDoorAsStaffIn> {
    override val name = "OpenCampaignCalendarDoorAsStaffIn"

    override fun read(r: PacketReader): OpenCampaignCalendarDoorAsStaffIn = readOpenCampaignCalendarDoorAsStaffIn(r)

    override fun write(w: PacketWriter, m: OpenCampaignCalendarDoorAsStaffIn) = writeOpenCampaignCalendarDoorAsStaffIn(w, m)

    private fun readOpenCampaignCalendarDoorAsStaffIn(r: PacketReader): OpenCampaignCalendarDoorAsStaffIn {
        val vCampaignName = r.string()
        val vDay = r.int()
        return OpenCampaignCalendarDoorAsStaffIn(vCampaignName, vDay)
    }

    private fun writeOpenCampaignCalendarDoorAsStaffIn(w: PacketWriter, m: OpenCampaignCalendarDoorAsStaffIn) {
        w.string(m.campaignName)
        w.int(m.day)
    }
}
