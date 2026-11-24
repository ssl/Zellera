package zel.protocol.generated

data class CampaignCalendarDataOut(
    val string1: String,
    val string2: String,
    val int1: Int,
    val int2: Int,
    val list1: List<Int>,
    val list2: List<Int>,
)

object CampaignCalendarDataOutCodec : OutgoingCodec<CampaignCalendarDataOut> {
    override val name = "CampaignCalendarDataOut"

    override fun read(r: PacketReader): CampaignCalendarDataOut = readCampaignCalendarDataOut(r)

    override fun write(w: PacketWriter, m: CampaignCalendarDataOut) = writeCampaignCalendarDataOut(w, m)

    private fun readCampaignCalendarDataOut(r: PacketReader): CampaignCalendarDataOut {
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vList1 = List(r.countInt(4)) { r.int() }
        val vList2 = List(r.countInt(4)) { r.int() }
        return CampaignCalendarDataOut(vString1, vString2, vInt1, vInt2, vList1, vList2)
    }

    private fun writeCampaignCalendarDataOut(w: PacketWriter, m: CampaignCalendarDataOut) {
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int1)
        w.int(m.int2)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.countInt(m.list2.size)
        m.list2.forEach { w.int(it) }
    }
}
