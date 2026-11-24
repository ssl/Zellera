package zel.protocol.quests

import zel.net.clients.ClientConnection
import zel.protocol.generated.QuestCancelledOutCodec
import zel.protocol.generated.QuestCompletedOutCodec
import zel.protocol.generated.QuestDailyOut
import zel.protocol.generated.QuestDailyOutCodec
import zel.protocol.generated.QuestOutCodec
import zel.protocol.generated.QuestsOut
import zel.protocol.generated.QuestsOutCodec
import zel.protocol.generated.SeasonalQuestsOut
import zel.protocol.generated.SeasonalQuestsOutCodec

class QuestMessages {
    fun reply(
        client: ClientConnection,
        reply: QuestReply,
    ) {
        when (reply) {
            is QuestReply.Tracked -> {
                client.send(QuestOutCodec, reply.quest.toTracked())
            }

            is QuestReply.Stopped -> {
                client.send(QuestCancelledOutCodec, reply.quest.toCancelled(reply.expired))
            }

            is QuestReply.Completed -> {
                client.send(QuestCompletedOutCodec, reply.quest.toCompleted(reply.showDialog))
            }
        }
    }

    fun list(
        client: ClientConnection,
        quests: List<QuestView>,
        openWindow: Boolean,
    ) = client.send(QuestsOutCodec, QuestsOut(quests.map { it.toListed() }, openWindow))

    fun seasonal(
        client: ClientConnection,
        quests: List<QuestView>,
    ) = client.send(SeasonalQuestsOutCodec, SeasonalQuestsOut(quests.map { it.toSeasonal() }))

    fun daily(
        client: ClientConnection,
        daily: DailyQuestView,
    ) {
        val quest = daily.quest
        val message =
            if (quest == null) {
                QuestDailyOut(false, QuestDailyOut.Branch1.Else)
            } else {
                QuestDailyOut(true, quest.toDaily(daily.easyOpenCount, daily.hardOpenCount))
            }
        client.send(QuestDailyOutCodec, message)
    }
}
