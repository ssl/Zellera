package zel.room

import zel.core.Configuration
import zel.ext.GameExtensions
import zel.game.achievements.AchievementProgress
import zel.game.catalog.Catalog
import zel.game.earnings.WiredCreditEarnings
import zel.game.items.Item
import zel.game.items.ItemServices
import zel.game.items.OwnedFurni
import zel.game.items.YoutubePlaylists
import zel.game.moderation.WordFilter
import zel.game.navigation.Navigator
import zel.game.permissions.Permissions
import zel.game.players.inventory.BadgeLeaderboard
import zel.game.players.level.AccountLevels
import zel.net.clients.ClientConnections
import zel.room.chat.ChatHost
import zel.room.listing.RoomListings
import zel.room.unit.bot.UnitHost
import zel.security.SuspicionLog
import javax.sql.DataSource

data class RoomEngineServices(
    val config: Configuration,
    val dataSource: DataSource,
    val clients: ClientConnections,
    val definitions: (Int) -> Item?,
    val persist: (Runnable) -> Unit,
    val persisted: () -> Boolean = { true },
    /** Database work that runs after every earlier job handed in under an equal key. */
    val persistInOrder: (key: Any, job: Runnable) -> Unit = { _, job -> persist(job) },
    val inventoryItem: (Int) -> OwnedFurni?,
    val progress: (playerId: Int, event: String, amount: Int) -> Unit = { _, _, _ -> },
    val achievements: () -> AchievementProgress = { AchievementProgress.NONE },
    val items: () -> ItemServices? = { null },
    val chat: ChatHost = ChatHost.None,
    val units: UnitHost = UnitHost.None,
    val guildFields: (Int) -> List<String>? = { null },
    val accountLevels: () -> AccountLevels? = { null },
    val leaderboard: () -> BadgeLeaderboard? = { null },
    val badgeExists: (String) -> Boolean = { false },
    val catalog: () -> Catalog? = { null },
    val listings: RoomListings,
    val navigator: () -> Navigator? = { null },
    val youtube: () -> YoutubePlaylists? = { null },
    val wordFilter: () -> WordFilter? = { null },
    val wiredEarnings: () -> WiredCreditEarnings? = { null },
    val definitionByCode: (String) -> Item? = { null },
    val permissions: () -> Permissions? = { null },
    val extensions: GameExtensions = GameExtensions.None,
    val suspicions: SuspicionLog = SuspicionLog.None,
) {
    /** Runs off the room's lock, after that player's earlier achievement work. */
    fun progressLater(
        playerId: Int,
        job: Runnable,
    ) = persistInOrder(AchievementProgress.queueKey(playerId), job)
}
