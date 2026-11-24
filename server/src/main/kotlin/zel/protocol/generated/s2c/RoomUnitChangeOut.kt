package zel.protocol.generated

data class RoomUnitChangeOut(
    val roomIndex: Int,
    val figure: String,
    val gender: String,
    val motto: String,
    val achievementScore: Int,
    val extra: String,
    val extraEntries: List<ExtraEntriesItem>,
    val badgesRank: Int,
) {
    data class ExtraEntriesItem(
        val first: Int,
        val second: Int,
        val third: Int,
    )
}

object RoomUnitChangeOutCodec : OutgoingCodec<RoomUnitChangeOut> {
    override val name = "RoomUnitChangeOut"

    override fun read(r: PacketReader): RoomUnitChangeOut = readRoomUnitChangeOut(r)

    override fun write(w: PacketWriter, m: RoomUnitChangeOut) = writeRoomUnitChangeOut(w, m)

    private fun readRoomUnitChangeOutExtraEntriesItem(r: PacketReader): RoomUnitChangeOut.ExtraEntriesItem {
        val vFirst = r.int()
        val vSecond = r.int()
        val vThird = r.int()
        return RoomUnitChangeOut.ExtraEntriesItem(vFirst, vSecond, vThird)
    }

    private fun readRoomUnitChangeOut(r: PacketReader): RoomUnitChangeOut {
        val vRoomIndex = r.int()
        val vFigure = r.string()
        val vGender = r.string()
        val vMotto = r.string()
        val vAchievementScore = r.int()
        val vExtra = r.string()
        val vExtraEntries = List(r.countInt(12)) { readRoomUnitChangeOutExtraEntriesItem(r) }
        val vBadgesRank = r.int()
        return RoomUnitChangeOut(vRoomIndex, vFigure, vGender, vMotto, vAchievementScore, vExtra, vExtraEntries, vBadgesRank)
    }

    private fun writeRoomUnitChangeOutExtraEntriesItem(w: PacketWriter, m: RoomUnitChangeOut.ExtraEntriesItem) {
        w.int(m.first)
        w.int(m.second)
        w.int(m.third)
    }

    private fun writeRoomUnitChangeOut(w: PacketWriter, m: RoomUnitChangeOut) {
        w.int(m.roomIndex)
        w.string(m.figure)
        w.string(m.gender)
        w.string(m.motto)
        w.int(m.achievementScore)
        w.string(m.extra)
        w.countInt(m.extraEntries.size)
        m.extraEntries.forEach { writeRoomUnitChangeOutExtraEntriesItem(w, it) }
        w.int(m.badgesRank)
    }
}
