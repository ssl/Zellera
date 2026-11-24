package zel.game.quests
import zel.game.players.Player
import zel.game.players.PlayerSaveQueue
import zel.game.players.PlayerStates
import zel.protocol.quests.DailyQuestView
import zel.protocol.quests.QuestMessages
import zel.protocol.quests.QuestReply
import zel.protocol.quests.QuestView
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** The quest window: one current quest per campaign, one reward per completion; changes run under player state. */
class Quests(
    private val records: QuestRecords,
    persistence: (Runnable) -> Unit,
    private val unixTime: () -> Int,
    private val messages: QuestMessages,
    private val rewards: QuestRewards,
    private val finishedCampaignQuest: (Player, String) -> Unit = { _, _ -> },
) {
    @Volatile
    private var book = QuestBook(emptyList())

    private val players =
        PlayerStates<PlayerQuests>(PlayerSaveQueue(persistence), { true }) { playerId, _ ->
            PlayerQuests(playerId, records.rowsOf(playerId))
        }

    fun reload() {
        book = QuestBook(records.definitions())
    }

    val hasNoQuests: Boolean get() = book.isEmpty

    fun knows(questId: Int): Boolean = questId in book

    fun unload(playerId: Int) = players.unload(playerId)

    /** Accepts [questId] if current in its chain; a new acceptance answers list and widget, a repeat the widget. */
    fun accept(
        player: Player,
        questId: Int,
    ): Boolean {
        val quest = book[questId] ?: return false
        val now = unixTime()
        val acceptance =
            withQuests(player) { mine ->
                val row = mine[questId]?.takeUnless { quest.isDaily && it.isCompleted }
                when {
                    !isCurrent(quest, mine) -> {
                        Acceptance.REFUSED
                    }

                    row?.accepted == true -> {
                        Acceptance.ALREADY
                    }

                    else -> {
                        remember(mine, (row ?: QuestEntry(questId)).copy(trackedSince = now))
                        Acceptance.NEW
                    }
                }
            }
        if (acceptance == Acceptance.REFUSED) return false
        player.client?.let { client ->
            if (acceptance == Acceptance.NEW) messages.list(client, fullList(player), false)
            messages.daily(client, dailyWidget(player, true, 0))
        }
        return true
    }

    /** The accept button: seasonal quests are ignored; others are accepted and answered as the tracked quest. */
    fun acceptFromWindow(
        player: Player,
        questId: Int,
    ) {
        val quest = book[questId] ?: return
        if (book.campaignOf(quest)?.isTimed == true) return
        val accepted = withQuests(player) { mine -> mine[questId]?.accepted == true }
        if (!accepted && !accept(player, questId)) return
        val now = unixTime()
        send(player, listOf(QuestReply.Tracked(withQuests(player) { mine -> viewOf(quest, mine, now) })))
    }

    fun dropDaily(player: Player): Boolean = stopTracked(player) { book[it.quest]?.isDaily == true }

    /** The cancel link: unaccepts a followed campaign quest, keeping progress; seasonal and daily ones are ignored. */
    fun reject(
        player: Player,
        questId: Int,
    ) {
        val quest = book[questId]?.takeUnless { it.isDaily } ?: return
        if (book.campaignOf(quest)?.isTimed != false) return
        val now = unixTime()
        val stopped =
            withQuests(player) { mine ->
                mine[questId]?.takeIf { it.accepted }?.let { untrack(mine, it, now) }
            } ?: return
        send(player, listOf(stopped))
        player.client?.let { messages.list(it, fullList(player), false) }
    }

    /** Accepts the campaign's current quest and answers it as the tracked quest, also when it was accepted before. */
    fun beginCampaign(
        player: Player,
        campaignCode: String,
    ) {
        val campaign = book.campaign(campaignCode) ?: return
        val next = withQuests(player) { mine -> currentOf(campaign, mine) } ?: return
        if (!accept(player, next.id)) return
        val now = unixTime()
        send(player, listOf(QuestReply.Tracked(withQuests(player) { mine -> viewOf(next, mine, now) })))
    }

    /** Counts [amount] on accepted quests whose goal admits [subject]; a finished campaign quest accepts the next. */
    fun count(
        player: Player,
        goal: CountedAction,
        amount: Int,
        subject: QuestSubject? = null,
    ) {
        val now = unixTime()
        val outcomes =
            withQuests(player) { mine ->
                mine.accepted.mapNotNull { row ->
                    val quest =
                        book[row.quest]?.takeIf { it.goal.type == goal && it.goal.admits(subject) }
                            ?: return@mapNotNull null
                    val progress = minOf(row.progress + amount, quest.goal.count)
                    val finished = progress >= quest.goal.count
                    val changed =
                        if (finished) {
                            row.copy(progress = progress, trackedSince = 0, completedAt = now)
                        } else {
                            row.copy(progress = progress)
                        }
                    remember(mine, changed)
                    val view = viewOf(quest, mine, now)
                    val reply =
                        if (finished) {
                            QuestReply.Completed(view.copy(accepted = true), true)
                        } else {
                            QuestReply.Tracked(view)
                        }
                    Outcome(
                        quest,
                        reply,
                        finished,
                        finished && acceptNext(quest, mine, now),
                    )
                }
            }
        outcomes.forEach { outcome ->
            if (outcome.finished) {
                pay(player, outcome.quest)
                finishedCampaignQuest(player, outcome.quest.slot.campaign)
            }
            send(player, listOf(outcome.reply))
            if (outcome.nextAccepted) player.client?.let { messages.list(it, fullList(player), false) }
            if (outcome.finished) PlayerActions.finishedQuest(player)
        }
    }

    private fun acceptNext(
        done: Quest,
        mine: PlayerQuests,
        now: Int,
    ): Boolean {
        if (done.isDaily) return false
        val next = book.campaignOf(done)?.let { currentOf(it, mine) } ?: return false
        val row = mine[next.id] ?: QuestEntry(next.id)
        if (!row.accepted) remember(mine, row.copy(trackedSince = now))
        return true
    }

    fun campaignList(player: Player): List<QuestView> {
        val now = unixTime()
        return withQuests(player) { mine ->
            book.ongoing.map { campaign ->
                currentOf(campaign, mine)?.let { viewOf(it, mine, now) }
                    ?: QuestStanding.campaignCompleted(campaign.code, campaign.quests.size)
            }
        }
    }

    /** Every campaign's current quest, seasonal ones too: the list an acceptance answers. */
    fun fullList(player: Player): List<QuestView> {
        val now = unixTime()
        return withQuests(player) { mine ->
            (book.ongoing + book.timed).mapNotNull { campaign ->
                currentOf(campaign, mine)?.let { viewOf(it, mine, now) }
            }
        }
    }

    fun seasonalList(player: Player): List<QuestView> {
        val now = unixTime()
        return withQuests(player) { mine ->
            book.timed.mapNotNull { campaign -> currentOf(campaign, mine)?.let { viewOf(it, mine, now) } }
        }
    }

    /** The first accepted quest, else the open daily of the asked kind at [index] (wrapping), with the open counts. */
    fun dailyWidget(
        player: Player,
        easy: Boolean,
        index: Int,
    ): DailyQuestView {
        val now = unixTime()
        return withQuests(player) { mine ->
            val open = book.dailies.filter { !doneToday(mine[it.id]) }
            val tracked = mine.tracked?.let { book[it.quest] }
            val ofKind = open.filter { it.isEasy == easy }
            val shown = tracked ?: ofKind.takeIf { it.isNotEmpty() }?.let { it[Math.floorMod(index, it.size)] }
            DailyQuestView(shown?.let { viewOf(it, mine, now) }, open.count { it.isEasy }, open.count { !it.isEasy })
        }
    }

    /** The quest window's list; a request from outside a room stays unanswered. */
    fun showCampaigns(
        player: Player,
        openWindow: Boolean,
    ) {
        if (book.ongoing.isEmpty() || !player.inRoom) return
        player.client?.let { messages.list(it, campaignList(player), openWindow) }
    }

    fun showSeasonal(player: Player) {
        player.client?.let { messages.seasonal(it, seasonalList(player)) }
    }

    fun showDaily(
        player: Player,
        easy: Boolean,
        index: Int,
    ) {
        player.client?.let { messages.daily(it, dailyWidget(player, easy, index)) }
    }

    fun showTracked(player: Player) {
        val now = unixTime()
        val view =
            withQuests(player) { mine ->
                mine.tracked?.let { row -> book[row.quest]?.let { viewOf(it, mine, now) } }
            } ?: return
        send(player, listOf(QuestReply.Tracked(view)))
    }

    private enum class Acceptance { REFUSED, ALREADY, NEW }

    private class Outcome(
        val quest: Quest,
        val reply: QuestReply,
        val finished: Boolean,
        val nextAccepted: Boolean,
    )

    private fun <T> withQuests(
        player: Player,
        change: (PlayerQuests) -> T,
    ): T = players.change(player.profile.id, change)

    private fun stopTracked(
        player: Player,
        matches: (QuestEntry) -> Boolean,
    ): Boolean {
        val now = unixTime()
        val replies =
            withQuests(player) { mine -> mine.accepted.filter(matches).mapNotNull { untrack(mine, it, now) } }
        send(player, replies)
        return replies.isNotEmpty()
    }

    private fun untrack(
        mine: PlayerQuests,
        row: QuestEntry,
        now: Int,
    ): QuestReply? {
        val quest = book[row.quest] ?: return null
        remember(mine, row.copy(trackedSince = 0))
        return QuestReply.Stopped(false, viewOf(quest, mine, now))
    }

    private fun remember(
        mine: PlayerQuests,
        entry: QuestEntry,
    ) {
        mine.put(entry)
        players.write(mine.playerId) { records.store(mine.playerId, entry) }
    }

    private fun pay(
        player: Player,
        quest: Quest,
    ) {
        rewards.grant(player, quest.reward)
        if (quest.badge.isNotBlank()) rewards.badge(player, quest.badge)
    }

    private fun send(
        player: Player,
        replies: List<QuestReply>,
    ) {
        val client = player.client ?: return
        replies.forEach { messages.reply(client, it) }
    }

    /** The first quest of the campaign the player has not completed; a daily quest is current until done today. */
    private fun currentOf(
        campaign: QuestBook.Campaign,
        mine: PlayerQuests,
    ): Quest? = campaign.quests.firstOrNull { mine[it.id]?.isCompleted != true }

    private fun isCurrent(
        quest: Quest,
        mine: PlayerQuests,
    ): Boolean =
        if (quest.isDaily) {
            !doneToday(mine[quest.id])
        } else {
            book.campaignOf(quest)?.let { currentOf(it, mine)?.id == quest.id } ?: false
        }

    private fun doneToday(row: QuestEntry?): Boolean = row != null && row.completedAt >= startOfToday()

    /** A daily quest counts as done only today; any other once completed. */
    private fun isCompleted(
        quest: Quest,
        row: QuestEntry?,
    ): Boolean = if (quest.isDaily) doneToday(row) else row?.isCompleted == true

    private fun startOfToday(): Long =
        LocalDate
            .ofInstant(Instant.ofEpochSecond(unixTime().toLong()), ZoneOffset.UTC)
            .atStartOfDay(ZoneOffset.UTC)
            .toEpochSecond()

    private fun viewOf(
        quest: Quest,
        mine: PlayerQuests,
        now: Int,
    ): QuestView {
        val inCampaign = book.campaignOf(quest)?.quests ?: listOf(quest)
        val row = mine[quest.id]
        val standing =
            QuestStanding(
                counted = row?.progress ?: 0,
                isTracked = row?.accepted == true,
                completedInCampaign = inCampaign.count { isCompleted(it, mine[it.id]) },
                campaignLength = inCampaign.size,
            )
        return standing.toView(quest, now)
    }
}
