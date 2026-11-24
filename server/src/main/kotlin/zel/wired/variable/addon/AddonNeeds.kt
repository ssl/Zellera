package zel.wired.variable.addon

import zel.room.Room
import zel.room.RoomScope
import zel.wired.BoxSettingsSource
import zel.wired.store.RoomLog
import zel.wired.store.WiredWriter
import zel.wired.timing.RoomPoster
import zel.wired.variable.RoomVariables
import javax.sql.DataSource

class AddonNeeds(
    val forms: BoxSettingsSource,
    val rows: ImportRows,
    val variables: () -> RoomVariables,
    val poster: RoomPoster,
    val persist: (Runnable) -> Unit,
    val writer: WiredWriter,
    val dataSource: DataSource,
    val secret: String,
    val timezone: (Room) -> String,
    val team: (RoomScope, Int) -> Int?,
    val codeOfBase: (Int) -> String?,
    val roomLog: (Room, RoomLog.Line) -> Unit = { _, _ -> },
)
