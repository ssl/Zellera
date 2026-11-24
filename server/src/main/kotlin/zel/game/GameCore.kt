package zel.game

import zel.core.CommandLog
import zel.core.ServerState
import zel.core.Texts
import zel.db.Database
import zel.game.guilds.BadgeImager
import zel.net.clients.ClientConnections
import java.time.Instant

/** [clients] and [badgeImager] exist only after the network phase, so they are read when used. */
class GameCore(
    val database: Database,
    val texts: Texts,
    val commandLog: CommandLog,
    val state: ServerState,
    val info: ServerInfo,
    val clients: () -> ClientConnections?,
    val badgeImager: () -> BadgeImager?,
    val now: () -> Int = { Instant.now().epochSecond.toInt() },
)
