package zel.game.moderation

import zel.core.Configuration
import zel.core.Texts
import zel.core.threading.GameThreads
import zel.game.items.OwnedFurni
import zel.game.permissions.Permissions
import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.game.players.Players
import zel.game.players.connection
import zel.net.clients.ClientConnections
import zel.protocol.outgoing.moderation.ModerationOutbox
import zel.protocol.outgoing.moderation.ModerationReplies
import zel.room.port.RoomPort
import javax.sql.DataSource

@Suppress("LongParameterList") // the services the parts are built from
class ModerationService(
    dataSource: DataSource,
    config: Configuration,
    private val texts: Texts,
    players: Players,
    permissions: Permissions,
    rooms: () -> RoomPort,
    clients: () -> ClientConnections,
    threading: GameThreads,
    private val outbox: ModerationOutbox,
    loadPhoto: (Int) -> OwnedFurni?,
    private val clock: () -> Int,
) {
    val catalog = ModerationCatalog(dataSource)
    val standing = StandingCounters(dataSource)
    val tickets = TicketDesk(dataSource, threading, outbox, standing, loadPhoto, clock)
    val chats = ChatRecords(dataSource)
    val visits = VisitRecords(dataSource, chats, clock)
    val reach = StaffReach(players, permissions)
    val mutes = StaffMutes(dataSource, players, config, clock)
    val tradeLocks = TradeLocks(dataSource, standing, config)
    val notice = SanctionNotice(texts::textOr, catalog::topic)
    val banning = PlayerBanning(dataSource, players, reach, standing, clients, threading, notice, config, clock)
    val actions = StaffActions(config, texts, players, reach, rooms)

    val myReports = MyReportsTable(dataSource)

    fun gameText(
        key: String,
        fallback: String,
    ): String = texts.textOr(key, fallback)

    fun sendLoginData(player: Player) {
        outbox.cfhTopics(player.connection, catalog)
        if (!player.hasPermission(Privilege.MODERATION_TOOL)) return
        sendModeratorInit(player)
        val stats = player.stats
        if (stats.settings.moderatorWindow.width > 0) {
            ModerationReplies.toolWindow(
                player.connection,
                stats.settings.moderatorWindow.x,
                stats.settings.moderatorWindow.y,
                stats.settings.moderatorWindow.width,
                stats.settings.moderatorWindow.height,
            )
        }
    }

    fun sendModeratorInit(player: Player) = outbox.moderatorInit(player, tickets.open(), catalog, clock())
}
