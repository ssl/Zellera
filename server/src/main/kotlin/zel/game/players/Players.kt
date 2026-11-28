package zel.game.players

import org.slf4j.LoggerFactory
import zel.db.SqlFailure
import zel.game.icons.IconService
import zel.game.permissions.PermissionActor
import zel.game.players.inventory.UnseenItems
import zel.game.players.inventory.UnseenItemsDatabase
import zel.server.GameServices
import java.sql.ResultSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

private val log = LoggerFactory.getLogger(Players::class.java)

class Players(
    private val services: GameServices,
    private val persistenceExecutor: Executor = Executor { it.run() },
    private val iconService: IconService? = null,
) {
    private val online = ConcurrentHashMap<Int, Player>()
    private val byName = ConcurrentHashMap<String, Player>()
    private val disconnectSaves = DisconnectSaves(persistenceExecutor)
    private val login = PlayerLogin(services, this, disconnectSaves)
    private val devices by lazy { PlayerDevices(services.database.queries, persistenceExecutor) }
    private val unseenStore: UnseenItems.Store =
        UnseenItemsDatabase(services, PlayerSaveQueue(persistenceExecutor::execute))

    val all: Collection<Player> get() = online.values

    val count: Int get() = online.size

    fun isOnline(id: Int): Boolean = online.containsKey(id)

    fun register(player: Player) {
        online[player.profile.id] = player
        byName[nameKey(player.profile.name)] = player
    }

    fun unregister(player: Player) {
        online.remove(player.profile.id, player)
        byName.remove(nameKey(player.profile.name), player)
    }

    fun renamed(
        player: Player,
        oldName: String,
    ) {
        byName.remove(nameKey(oldName), player)
        byName[nameKey(player.profile.name)] = player
    }

    fun find(id: Int): Player? = online[id]

    fun find(name: String): Player? = byName[nameKey(name)]

    fun newPlayer(row: ResultSet): Player = Player(services, unseenStore, row, iconService)

    fun readProfile(id: Int): PlayerProfile? = readOne("p.id = ?", id)

    fun readProfile(name: String): PlayerProfile? = readOne("p.name = ?", name)

    fun profileOf(id: Int): PlayerProfile? = find(id)?.profile ?: readProfile(id)

    fun logIn(
        ticket: String,
        device: DeviceReading?,
        address: String? = null,
    ): TicketLogin = login.logIn(ticket, device, address)

    /** Written by the database threads, not by the login. */
    fun loggedInFrom(
        player: Player,
        device: DeviceReading,
    ) = devices.loggedIn(player.profile.id, device)

    fun beginDisconnectSave(playerId: Int): DisconnectSaves.Save = disconnectSaves.begin(playerId)

    fun changeRank(
        userId: Int,
        rankId: Int,
        actorId: Int = PermissionActor.SYSTEM_ID,
        actorName: String = "system",
        via: String = "system",
    ) = PlayerRanks(services, this).change(userId, rankId, actorId, actorName, via)

    fun reloadRank(player: Player) = PlayerRanks(services, this).reload(player)

    fun dispose() {
        online.values.toList().forEach { it.disconnect() }
        online.clear()
        byName.clear()
    }

    private fun readOne(
        where: String,
        key: Any,
    ): PlayerProfile? =
        try {
            services.database.queries.select("${PlayerAccountRow.SELECT} WHERE $where LIMIT 1", key).first {
                PlayerProfile(services, it)
            }
        } catch (e: SqlFailure) {
            log.error("A profile could not be read ({} = {})", where, key, e)
            null
        }

    private fun nameKey(name: String) = name.lowercase()
}
