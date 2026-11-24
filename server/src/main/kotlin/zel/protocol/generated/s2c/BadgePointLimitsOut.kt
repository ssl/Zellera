package zel.protocol.generated

data class BadgePointLimitsOut(
    val badges: List<BadgesItem>,
) {
    data class BadgesItem(
        val badgeCode: String,
        val levels: List<LevelsItem>,
    ) {
        data class LevelsItem(
            val level: Int,
            val progress: Int,
        )
    }
}

object BadgePointLimitsOutCodec : OutgoingCodec<BadgePointLimitsOut> {
    override val name = "BadgePointLimitsOut"

    override fun read(r: PacketReader): BadgePointLimitsOut = readBadgePointLimitsOut(r)

    override fun write(w: PacketWriter, m: BadgePointLimitsOut) = writeBadgePointLimitsOut(w, m)

    private fun readBadgePointLimitsOutBadgesItemLevelsItem(r: PacketReader): BadgePointLimitsOut.BadgesItem.LevelsItem {
        val vLevel = r.int()
        val vProgress = r.int()
        return BadgePointLimitsOut.BadgesItem.LevelsItem(vLevel, vProgress)
    }

    private fun readBadgePointLimitsOutBadgesItem(r: PacketReader): BadgePointLimitsOut.BadgesItem {
        val vBadgeCode = r.string()
        val vLevels = List(r.countInt(8)) { readBadgePointLimitsOutBadgesItemLevelsItem(r) }
        return BadgePointLimitsOut.BadgesItem(vBadgeCode, vLevels)
    }

    private fun readBadgePointLimitsOut(r: PacketReader): BadgePointLimitsOut {
        val vBadges = List(r.countInt(6)) { readBadgePointLimitsOutBadgesItem(r) }
        return BadgePointLimitsOut(vBadges)
    }

    private fun writeBadgePointLimitsOutBadgesItemLevelsItem(w: PacketWriter, m: BadgePointLimitsOut.BadgesItem.LevelsItem) {
        w.int(m.level)
        w.int(m.progress)
    }

    private fun writeBadgePointLimitsOutBadgesItem(w: PacketWriter, m: BadgePointLimitsOut.BadgesItem) {
        w.string(m.badgeCode)
        w.countInt(m.levels.size)
        m.levels.forEach { writeBadgePointLimitsOutBadgesItemLevelsItem(w, it) }
    }

    private fun writeBadgePointLimitsOut(w: PacketWriter, m: BadgePointLimitsOut) {
        w.countInt(m.badges.size)
        m.badges.forEach { writeBadgePointLimitsOutBadgesItem(w, it) }
    }
}
