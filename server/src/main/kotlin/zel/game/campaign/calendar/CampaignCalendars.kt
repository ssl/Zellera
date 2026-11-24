package zel.game.campaign.calendar

import zel.content.ClientStrings
import zel.game.economy.EconomyLedger
import zel.game.items.Item
import zel.game.items.Items
import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.game.players.subscriptions.Subscription
import zel.game.quests.QuestRewards
import zel.protocol.events.CalendarMessages
import zel.util.GameZone
import zel.util.SECONDS_PER_DAY
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.Random

class CampaignCalendars(
    private val records: CalendarStore,
    private val items: Items,
    private val randomSource: () -> Random,
    private val unixTime: () -> Int,
    private val messages: CalendarMessages,
    private val today: () -> LocalDate = GameZone::today,
) {
    private val rewards = QuestRewards(items)

    @Volatile
    private var byName: Map<String, CalendarCampaign> = emptyMap()

    fun refresh() {
        byName = records.campaigns().associateBy { it.name }
    }

    fun dispose() {
        byName = emptyMap()
    }

    fun campaign(name: String): CalendarCampaign? = byName[name]

    fun show(
        player: Player,
        campaign: CalendarCampaign,
    ): Boolean {
        val client = player.client ?: return false
        val date = today()
        if (!campaign.isRunning(date)) return false
        val opened = records.openedDays(player.profile.id, campaign.id)
        messages.calendar(client, campaign, campaign.doorOf(date), opened, campaign.shutDoors(date, opened))
        return true
    }

    fun openDoor(
        player: Player,
        name: String,
        day: Int,
        asStaff: Boolean,
    ) {
        val client = player.client ?: return
        val campaign = byName[name] ?: return
        val forced = asStaff && player.hasPermission(Privilege.CALENDAR_FORCE_OPEN)
        if (!campaign.opens(day, today(), forced)) return
        val prize = campaign.prizeFor(day, randomSource()) ?: return
        if (!records.claim(player.profile.id, campaign.id, day, prize.id, unixTime())) return
        val furni = prize.furniId?.let(items::getItem)
        handOver(player, prize, furni)
        val text = prize.doorText(furni?.name)
        messages.doorOpened(client, true, text, prize.image, furni?.name.orEmpty())
    }

    private fun handOver(
        player: Player,
        prize: CalendarPrize,
        furni: Item?,
    ) {
        when (val currency = prize.currency?.takeIf { prize.pays }) {
            null -> Unit
            EconomyLedger.CREDITS -> player.wallet.changeCredits(prize.amount, PRIZE_CAUSE)
            else -> player.wallet.changePoints(currency, prize.amount, PRIZE_CAUSE)
        }
        if (prize.badge.isNotBlank()) player.inventory.badges.award(prize.badge)
        if (prize.givesClub) {
            val membership = membershipNamed(prize.clubKind)
            if (membership == null) {
                LOGGER.warn("Calendar prize {} names no membership: '{}'", prize.id, prize.clubKind)
            } else {
                player.stats.club.create(membership, prize.clubDays * SECONDS_PER_DAY)
            }
        }
        rewards.furni(player, furni)
    }

    private fun membershipNamed(word: String): String? =
        if (word == ClientStrings[ClientStrings.CLUB_TYPE]) Subscription.CLUB else Subscription.typeNamed(word)

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(CampaignCalendars::class.java)
        const val PRIZE_CAUSE = "calendar.prize"
    }
}
