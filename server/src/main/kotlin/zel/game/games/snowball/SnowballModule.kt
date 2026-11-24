package zel.game.games.snowball

import zel.core.Configuration
import zel.core.threading.GameThreads
import zel.game.achievements.Achievements
import zel.game.earnings.IncomeRewards
import zel.game.items.Items
import zel.game.messenger.FriendUpdates
import zel.game.moderation.WordFilter
import zel.game.players.Players
import zel.protocol.gamecenter.GameCenterPackets
import zel.protocol.snowball.SnowballPackets
import zel.protocol.typed.PacketPackage
import java.time.Clock
import java.util.function.Supplier
import javax.sql.DataSource

class SnowballModule(
    configuration: Configuration,
    threading: GameThreads,
    dataSource: DataSource,
    players: Players,
    achievements: Achievements,
    wordFilter: WordFilter,
    items: Items,
    rewards: IncomeRewards,
    friendUpdates: Supplier<FriendUpdates>,
    site: Supplier<String>,
    clock: Clock = Clock.systemUTC(),
) {
    private val scores = SnowballScoreTable(dataSource, clock)
    private val plays = SnowballPlayTable(dataSource, clock)
    private val tokens = SnowballTokens(plays)

    val lobby =
        SnowballLobby(
            SnowballGameHost(
                scores,
                plays,
                clock,
                configuration,
                threading,
                players,
                achievements,
                wordFilter,
                items,
                rewards,
                friendUpdates,
                site,
            ),
            SnowballLeaderboards(scores, clock),
        )

    fun packages(): List<PacketPackage> = listOf(GameCenterPackets(lobby, tokens), SnowballPackets(lobby))
}
