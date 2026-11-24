package zel.game.messenger

import zel.core.Configuration
import zel.core.threading.GameThreads
import zel.db.Sql
import zel.game.achievements.Achievements
import zel.game.moderation.WordFilter
import zel.game.players.Players
import zel.net.clients.ClientConnections
import zel.protocol.friends.FriendMessages
import java.util.function.Supplier
import javax.sql.DataSource

class MessengerModule(
    dataSource: DataSource,
    configuration: Configuration,
    val clock: () -> Int,
    threading: GameThreads,
    clients: Supplier<ClientConnections>,
    players: Players,
    achievements: Achievements,
    wordFilter: WordFilter,
    limits: MessengerLimits = MessengerLimits(),
) {
    private val sql = Sql(dataSource)

    val messages = FriendMessages(players, clock)
    val connected = ConnectedPlayers(clients)
    val friends = FriendStore(dataSource, sql, connected, clock)
    val requests = FriendRequestStore(sql, clock)
    val updates = FriendUpdates(messages)
    val friendships = Friendships(friends, connected, updates, threading)
    val search =
        PlayerNameSearch(
            sql,
            connected,
            { configuration.getInt("messenger.search.max_results") },
        )
    val listAchievement = FriendListAchievement(achievements)
    val offlinePlayers = OfflinePlayers(sql)
    val blocks = PlayerBlocks(sql, connected)
    val history = MessengerHistory(sql)
    val chat =
        ChatDelivery(
            configuration,
            clock,
            threading,
            connected,
            wordFilter,
            messages,
            history,
            OfflineInbox(sql, limits.offlineDeliveryMax, clock),
        )

    fun newFriendList(): FriendList = FriendList(friends, connected, updates)
}
