package zel.protocol.generated

data class BadgesOut(
    val totalFragments: Int,
    val fragmentNo: Int,
    val badges: List<BadgesItem>,
) {
    data class BadgesItem(
        val badgeId: Int,
        val badgeCode: String,
        val ownerCount: Int,
        val rarity: Int,
    )
}

object BadgesOutCodec : OutgoingCodec<BadgesOut> {
    override val name = "BadgesOut"

    override fun read(r: PacketReader): BadgesOut = readBadgesOut(r)

    override fun write(w: PacketWriter, m: BadgesOut) = writeBadgesOut(w, m)

    private fun readBadgesOutBadgesItem(r: PacketReader): BadgesOut.BadgesItem {
        val vBadgeId = r.int()
        val vBadgeCode = r.string()
        val vOwnerCount = r.int()
        val vRarity = r.int()
        return BadgesOut.BadgesItem(vBadgeId, vBadgeCode, vOwnerCount, vRarity)
    }

    private fun readBadgesOut(r: PacketReader): BadgesOut {
        val vTotalFragments = r.int()
        val vFragmentNo = r.int()
        val vBadges = List(r.countInt(14)) { readBadgesOutBadgesItem(r) }
        return BadgesOut(vTotalFragments, vFragmentNo, vBadges)
    }

    private fun writeBadgesOutBadgesItem(w: PacketWriter, m: BadgesOut.BadgesItem) {
        w.int(m.badgeId)
        w.string(m.badgeCode)
        w.int(m.ownerCount)
        w.int(m.rarity)
    }

    private fun writeBadgesOut(w: PacketWriter, m: BadgesOut) {
        w.int(m.totalFragments)
        w.int(m.fragmentNo)
        w.countInt(m.badges.size)
        m.badges.forEach { writeBadgesOutBadgesItem(w, it) }
    }
}
