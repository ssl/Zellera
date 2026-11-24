package zel.protocol.quests

import zel.protocol.generated.QuestCancelledOut
import zel.protocol.generated.QuestCompletedOut
import zel.protocol.generated.QuestDailyOut
import zel.protocol.generated.QuestOut
import zel.protocol.generated.QuestsOut
import zel.protocol.generated.SeasonalQuestsOut

data class QuestView(
    val campaignCode: String,
    val completedQuestsInCampaign: Int,
    val questCountInCampaign: Int,
    val activityPointType: Int,
    val id: Int,
    val accepted: Boolean,
    val type: String,
    val imageVersion: String,
    val rewardCurrencyAmount: Int,
    val localizationCode: String,
    val completedSteps: Int,
    val totalSteps: Int,
    val sortOrder: Int,
    val catalogPageName: String,
    val chainCode: String,
    val easy: Boolean,
    val secondsLeft: Int? = null,
) {
    private val seasonal: Boolean get() = secondsLeft != null

    fun toTracked(): QuestOut =
        fields(secondsLeft?.let { QuestOut.Branch1.Then(it) } ?: QuestOut.Branch1.Else, ::QuestOut)

    fun toListed(): QuestsOut.List1Item =
        fields(
            secondsLeft?.let { QuestsOut.List1Item.Branch1.Then(it) } ?: QuestsOut.List1Item.Branch1.Else,
            QuestsOut::List1Item,
        )

    fun toSeasonal(): SeasonalQuestsOut.List1Item =
        fields(
            secondsLeft?.let { SeasonalQuestsOut.List1Item.Branch1.Then(it) }
                ?: SeasonalQuestsOut.List1Item.Branch1.Else,
            SeasonalQuestsOut::List1Item,
        )

    fun toCompleted(showDialog: Boolean): QuestCompletedOut {
        val q = toTracked()
        return QuestCompletedOut(
            q.string1,
            q.int1,
            q.int2,
            q.int3,
            q.int4,
            q.boolean1,
            q.string2,
            q.string3,
            q.int5,
            q.string4,
            q.int6,
            q.int7,
            q.int8,
            q.string5,
            q.string6,
            q.boolean2,
            q.boolean3,
            secondsLeft?.let { QuestCompletedOut.Branch1.Then(it) } ?: QuestCompletedOut.Branch1.Else,
            showDialog,
        )
    }

    fun toCancelled(expired: Boolean): QuestCancelledOut {
        val q = toTracked()
        return QuestCancelledOut(
            expired,
            q.string1,
            q.int1,
            q.int2,
            q.int3,
            q.int4,
            q.boolean1,
            q.string2,
            q.string3,
            q.int5,
            q.string4,
            q.int6,
            q.int7,
            q.int8,
            q.string5,
            q.string6,
            q.boolean2,
            q.boolean3,
            secondsLeft?.let { QuestCancelledOut.Branch1.Then(it) } ?: QuestCancelledOut.Branch1.Else,
        )
    }

    fun toDaily(
        easyOpen: Int,
        hardOpen: Int,
    ): QuestDailyOut.Branch1.Then {
        val q = toTracked()
        return QuestDailyOut.Branch1.Then(
            q.string1,
            q.int1,
            q.int2,
            q.int3,
            q.int4,
            q.boolean1,
            q.string2,
            q.string3,
            q.int5,
            q.string4,
            q.int6,
            q.int7,
            q.int8,
            q.string5,
            q.string6,
            q.boolean2,
            q.boolean3,
            secondsLeft?.let {
                QuestDailyOut.Branch1.Then.Branch1
                    .Then(it)
            } ?: QuestDailyOut.Branch1.Then.Branch1.Else,
            easyOpen,
            hardOpen,
        )
    }

    private fun <B, T> fields(
        branch: B,
        build: (
            String,
            Int,
            Int,
            Int,
            Int,
            Boolean,
            String,
            String,
            Int,
            String,
            Int,
            Int,
            Int,
            String,
            String,
            Boolean,
            Boolean,
            B,
        ) -> T,
    ): T =
        build(
            campaignCode,
            completedQuestsInCampaign,
            questCountInCampaign,
            activityPointType,
            id,
            accepted,
            type,
            imageVersion,
            rewardCurrencyAmount,
            localizationCode,
            completedSteps,
            totalSteps,
            sortOrder,
            catalogPageName,
            chainCode,
            easy,
            seasonal,
            branch,
        )
}
