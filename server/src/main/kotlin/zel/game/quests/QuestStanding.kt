package zel.game.quests

import zel.protocol.quests.QuestView

/** A player's standing: [counted] of a quest's goal so far, [completedInCampaign] of [campaignLength] quests. */
internal data class QuestStanding(
    val counted: Int,
    val isTracked: Boolean,
    val completedInCampaign: Int,
    val campaignLength: Int,
) {
    fun toView(
        quest: Quest,
        now: Int,
    ): QuestView {
        val (currency, pays) = quest.reward
        return QuestView(
            campaignCode = quest.slot.campaign,
            completedQuestsInCampaign = completedInCampaign,
            questCountInCampaign = campaignLength,
            activityPointType = currency,
            id = quest.id,
            accepted = isTracked,
            type = quest.goal.type.name,
            imageVersion = quest.display.imageVersion,
            rewardCurrencyAmount = pays,
            localizationCode = quest.code,
            completedSteps = counted.coerceAtMost(quest.goal.count),
            totalSteps = quest.goal.count,
            sortOrder = quest.slot.position,
            catalogPageName = quest.display.catalogPage,
            chainCode = quest.slot.chain,
            easy = quest.isEasy,
            secondsLeft = quest.countdown(now),
        )
    }

    companion object {
        /** A campaign with every quest completed: the client reads id 0 as "campaign completed". */
        fun campaignCompleted(
            code: String,
            length: Int,
        ): QuestView =
            QuestView(
                campaignCode = code,
                completedQuestsInCampaign = length,
                questCountInCampaign = length,
                activityPointType = 0,
                id = 0,
                accepted = false,
                type = "",
                imageVersion = "",
                rewardCurrencyAmount = 0,
                localizationCode = "",
                completedSteps = 0,
                totalSteps = 0,
                sortOrder = 0,
                catalogPageName = "",
                chainCode = "",
                easy = false,
            )
    }
}
