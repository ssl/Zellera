package zel.protocol.quests

import zel.game.quests.DailyTask
import zel.game.quests.DailyTaskCard
import zel.game.quests.QuestReward
import zel.net.clients.ClientConnection
import zel.protocol.generated.DailyTasksActiveListOut
import zel.protocol.generated.DailyTasksActiveListOutCodec
import zel.protocol.generated.DailyTasksTaskUpdateOut
import zel.protocol.generated.DailyTasksTaskUpdateOutCodec
import zel.protocol.generated.DailyTasksTasksAddedOut
import zel.protocol.generated.DailyTasksTasksAddedOutCodec

class DailyTaskMessages {
    fun active(
        client: ClientConnection,
        cards: List<DailyTaskCard>,
    ) = client.send(DailyTasksActiveListOutCodec, activeMessage(cards))

    fun added(
        client: ClientConnection,
        cards: List<DailyTaskCard>,
    ) = client.send(DailyTasksTasksAddedOutCodec, addedMessage(cards))

    fun updated(
        client: ClientConnection,
        taskId: Long,
        repeats: Int,
        status: Int,
        secondsLeft: Int,
    ) = client.send(
        DailyTasksTaskUpdateOutCodec,
        DailyTasksTaskUpdateOut(taskId, repeats, status.toByte(), secondsLeft),
    )

    companion object {
        /** Product type of a currency reward; its item type is then the activity point type. */
        private const val CURRENCY_PRODUCT: Short = 8

        fun activeMessage(cards: List<DailyTaskCard>) =
            DailyTasksActiveListOut(
                cards.map(::Line).map {
                    DailyTasksActiveListOut.List1Item(
                        it.id,
                        it.code,
                        it.type,
                        it.bonus,
                        it.image,
                        it.catalog,
                        it.goal,
                        it.done,
                        it.status,
                        it.left,
                        listOf(DailyTasksActiveListOut.List1Item.List1Item(it.product, it.item, it.extra, it.amount)),
                    )
                },
            )

        fun addedMessage(cards: List<DailyTaskCard>) =
            DailyTasksTasksAddedOut(
                cards.map(::Line).map {
                    DailyTasksTasksAddedOut.List1Item(
                        it.id,
                        it.code,
                        it.type,
                        it.bonus,
                        it.image,
                        it.catalog,
                        it.goal,
                        it.done,
                        it.status,
                        it.left,
                        listOf(DailyTasksTasksAddedOut.List1Item.List1Item(it.product, it.item, it.extra, it.amount)),
                    )
                },
            )

        private class Line(
            card: DailyTaskCard,
        ) {
            val id = card.listedId
            val code = card.task.code
            val type = card.task.listedType
            val bonus = card.task.isBonus
            val image = card.task.imageVersion
            val catalog = card.task.catalogName
            val goal = card.task.requiredRepeats
            val done = card.done
            val status = card.status.code.toByte()
            val left = card.secondsLeft
            val product: Short
            val item: String
            val extra: String
            val amount: Int

            init {
                val stored: DailyTask.StoredReward = card.task.reward
                when (val reward = stored.toQuestReward()) {
                    is QuestReward.Currency -> {
                        product = CURRENCY_PRODUCT
                        item = reward.pointType.toString()
                        extra = ""
                        amount = reward.amount
                    }

                    else -> {
                        product = 0
                        item = stored.type
                        extra = stored.extra
                        amount = stored.amount
                    }
                }
            }
        }
    }
}
