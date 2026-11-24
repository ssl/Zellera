package zel.protocol.quests

import zel.game.quests.DailyTasks
import zel.game.quests.Quests
import zel.protocol.generated.AcceptQuestInCodec
import zel.protocol.generated.ActivateQuestInCodec
import zel.protocol.generated.CancelQuestInCodec
import zel.protocol.generated.ClaimDailyTaskInCodec
import zel.protocol.generated.GetDailyQuestInCodec
import zel.protocol.generated.GetDailyTasksInCodec
import zel.protocol.generated.GetQuestsInCodec
import zel.protocol.generated.GetSeasonalQuestsOnlyInCodec
import zel.protocol.generated.OpenQuestTrackerInCodec
import zel.protocol.generated.RejectQuestInCodec
import zel.protocol.generated.StartCampaignInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.player

class QuestPackets(
    quests: Quests,
    dailyTasks: DailyTasks,
) : PacketPackage {
    override val name = "quests"

    override val routes =
        listOf(
            PacketRoute(
                AcceptQuestInCodec,
            ) { session, m -> quests.acceptFromWindow(session.player, m.questId) },
            PacketRoute(
                ActivateQuestInCodec,
            ) { session, m -> quests.accept(session.player, m.questId) },
            PacketRoute(CancelQuestInCodec) { session, _ -> quests.dropDaily(session.player) },
            PacketRoute(ClaimDailyTaskInCodec) { session, m -> dailyTasks.claim(session.player, m.wireId) },
            PacketRoute(GetDailyQuestInCodec) { session, m ->
                quests.showDaily(session.player, m.easy, m.index)
            },
            PacketRoute(GetDailyTasksInCodec) { session, _ -> dailyTasks.showCards(session.player) },
            PacketRoute(GetQuestsInCodec) { session, _ -> quests.showCampaigns(session.player, true) },
            PacketRoute(GetSeasonalQuestsOnlyInCodec) { session, _ ->
                quests.showSeasonal(session.player)
            },
            // the quest the tracker follows after a completion
            PacketRoute(
                OpenQuestTrackerInCodec,
            ) { session, _ -> quests.showTracked(session.player) },
            PacketRoute(RejectQuestInCodec) { session, m -> quests.reject(session.player, m.questId) },
            PacketRoute(StartCampaignInCodec) { session, m ->
                quests.beginCampaign(session.player, m.campaignCode)
            },
        )
}
