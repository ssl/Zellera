package zel.game.quests

import zel.core.Configuration
import zel.game.achievements.ExploreActivityAchievements
import zel.game.items.Items
import zel.game.players.Player
import zel.game.players.Players
import zel.game.rewardtracks.RewardTrackService
import zel.protocol.quests.DailyTaskMessages
import zel.protocol.quests.QuestMessages
import zel.protocol.quests.QuestPackets
import zel.protocol.typed.PacketPackage
import javax.sql.DataSource

class QuestsModule(
    dataSource: DataSource,
    configuration: Configuration,
    items: Items,
    players: Players,
    rewardTracks: RewardTrackService,
    explore: ExploreActivityAchievements,
    persistence: (Runnable) -> Unit,
    ordered: (Any, Runnable) -> Unit,
    unixTime: () -> Int,
    finishedCampaignQuest: (Player, String) -> Unit,
) {
    private val rewards = QuestRewards(items)
    val quests =
        Quests(StoredQuests(dataSource), persistence, unixTime, QuestMessages(), rewards, finishedCampaignQuest)
    val dailyTasks =
        DailyTasks(dataSource, configuration, persistence, unixTime, DailyTaskMessages(), rewards, explore)
    val packets: PacketPackage = QuestPackets(quests, dailyTasks)
    private val counting =
        OrderedCounting(ActionCounting(quests, dailyTasks, rewardTracks, players), ordered) { players.find(it) }

    init {
        quests.reload()
    }

    fun install() = PlayerActions.countWith(counting)
}
