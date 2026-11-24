package zel.protocol.generated

data class StartCampaignIn(
    val campaignCode: String,
)

object StartCampaignInCodec : IncomingCodec<StartCampaignIn> {
    override val name = "StartCampaignIn"

    override fun read(r: PacketReader): StartCampaignIn = readStartCampaignIn(r)

    override fun write(w: PacketWriter, m: StartCampaignIn) = writeStartCampaignIn(w, m)

    private fun readStartCampaignIn(r: PacketReader): StartCampaignIn {
        val vCampaignCode = r.string()
        return StartCampaignIn(vCampaignCode)
    }

    private fun writeStartCampaignIn(w: PacketWriter, m: StartCampaignIn) {
        w.string(m.campaignCode)
    }
}
