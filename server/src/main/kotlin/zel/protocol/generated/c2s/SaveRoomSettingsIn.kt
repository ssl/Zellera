package zel.protocol.generated

data class SaveRoomSettingsIn(
    val roomId: Int,
    val name: String,
    val description: String,
    val doorMode: Int,
    val password: String,
    val maximumVisitors: Int,
    val categoryId: Int,
    val tags: List<String>,
    val tradeMode: Int,
    val allowPets: Boolean,
    val allowFoodConsume: Boolean,
    val allowWalkThrough: Boolean,
    val hideWalls: Boolean,
    val wallThickness: Int,
    val floorThickness: Int,
    val whoCanMute: Int,
    val whoCanKick: Int,
    val whoCanBan: Int,
    val floodSensitivity: Int,
    val leaveOnDoorTile: Boolean,
    val idleSleep: Boolean,
    val idleSleepTimeout: Int,
    val idleAutokick: Boolean,
    val idleAutokickTimeout: Int,
    val muteAllPets: Boolean,
)

object SaveRoomSettingsInCodec : IncomingCodec<SaveRoomSettingsIn> {
    override val name = "SaveRoomSettingsIn"

    override fun read(r: PacketReader): SaveRoomSettingsIn = readSaveRoomSettingsIn(r)

    override fun write(w: PacketWriter, m: SaveRoomSettingsIn) = writeSaveRoomSettingsIn(w, m)

    private fun readSaveRoomSettingsIn(r: PacketReader): SaveRoomSettingsIn {
        val vRoomId = r.int()
        val vName = r.string()
        val vDescription = r.string()
        val vDoorMode = r.int()
        val vPassword = r.string()
        val vMaximumVisitors = r.int()
        val vCategoryId = r.int()
        val vTags = List(r.countInt(2)) { r.string() }
        val vTradeMode = r.int()
        val vAllowPets = r.boolean()
        val vAllowFoodConsume = r.boolean()
        val vAllowWalkThrough = r.boolean()
        val vHideWalls = r.boolean()
        val vWallThickness = r.int()
        val vFloorThickness = r.int()
        val vWhoCanMute = r.int()
        val vWhoCanKick = r.int()
        val vWhoCanBan = r.int()
        val vFloodSensitivity = r.int()
        val vLeaveOnDoorTile = r.boolean()
        val vIdleSleep = r.boolean()
        val vIdleSleepTimeout = r.int()
        val vIdleAutokick = r.boolean()
        val vIdleAutokickTimeout = r.int()
        val vMuteAllPets = r.boolean()
        return SaveRoomSettingsIn(vRoomId, vName, vDescription, vDoorMode, vPassword, vMaximumVisitors, vCategoryId, vTags, vTradeMode, vAllowPets, vAllowFoodConsume, vAllowWalkThrough, vHideWalls, vWallThickness, vFloorThickness, vWhoCanMute, vWhoCanKick, vWhoCanBan, vFloodSensitivity, vLeaveOnDoorTile, vIdleSleep, vIdleSleepTimeout, vIdleAutokick, vIdleAutokickTimeout, vMuteAllPets)
    }

    private fun writeSaveRoomSettingsIn(w: PacketWriter, m: SaveRoomSettingsIn) {
        w.int(m.roomId)
        w.string(m.name)
        w.string(m.description)
        w.int(m.doorMode)
        w.string(m.password)
        w.int(m.maximumVisitors)
        w.int(m.categoryId)
        w.countInt(m.tags.size)
        m.tags.forEach { w.string(it) }
        w.int(m.tradeMode)
        w.boolean(m.allowPets)
        w.boolean(m.allowFoodConsume)
        w.boolean(m.allowWalkThrough)
        w.boolean(m.hideWalls)
        w.int(m.wallThickness)
        w.int(m.floorThickness)
        w.int(m.whoCanMute)
        w.int(m.whoCanKick)
        w.int(m.whoCanBan)
        w.int(m.floodSensitivity)
        w.boolean(m.leaveOnDoorTile)
        w.boolean(m.idleSleep)
        w.int(m.idleSleepTimeout)
        w.boolean(m.idleAutokick)
        w.int(m.idleAutokickTimeout)
        w.boolean(m.muteAllPets)
    }
}
