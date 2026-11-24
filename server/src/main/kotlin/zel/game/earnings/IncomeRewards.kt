package zel.game.earnings

import org.slf4j.LoggerFactory
import zel.core.rules.rules
import zel.game.achievements.AchievementEvents
import zel.game.economy.EconomyLedger
import zel.game.items.Item
import zel.game.items.Items
import zel.game.items.OwnedFurni
import zel.game.players.Player
import zel.game.players.WalletWrites
import zel.net.clients.ClientConnection
import zel.protocol.generated.ActivityPointNotificationOut
import zel.protocol.generated.ActivityPointNotificationOutCodec
import zel.protocol.generated.IncomeRewardClaimResponseOut
import zel.protocol.generated.IncomeRewardClaimResponseOutCodec
import zel.protocol.generated.IncomeRewardNotificationOut
import zel.protocol.generated.IncomeRewardNotificationOutCodec
import zel.protocol.outgoing.earnings.IncomeRewardStatus
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.protocol.outgoing.inventory.AddRoomItem.UnseenCategory
import zel.protocol.outgoing.inventory.InventoryUpdateItem
import zel.protocol.outgoing.users.UserCredits
import zel.util.MILLIS_PER_SECOND
import java.sql.Connection
import java.sql.SQLException
import java.time.Clock
import java.time.LocalDate
import java.util.Collections
import java.util.WeakHashMap

@Suppress("TooManyFunctions") // listing, announcing and claiming are one workflow around the same store
class IncomeRewards(
    private val store: Store,
    private val clock: Clock,
    private val online: (Int) -> Player?,
    private val items: () -> Items?,
) {
    data class Reward(
        val id: Int,
        val category: Int,
        val type: Int,
        val amount: Int,
        val productCode: String,
    ) {
        val isFurni: Boolean get() = productCode.isNotEmpty()

        val currency: Int get() = if (type == CREDITS) EconomyLedger.CREDITS else EconomyLedger.POINTS
    }

    enum class Notice {
        NEW_EARNING,

        UNSEEN,
    }

    /** [balanceAfter] is -1 when no currency moved. */
    data class Paid(
        val reward: Reward,
        val balanceAfter: Int,
        val furni: OwnedFurni? = null,
    )

    /** Throws when the furni cannot be made, so the claim rolls back. */
    fun interface FurniMaker {
        @Throws(SQLException::class)
        fun make(
            connection: Connection,
            reward: Reward,
        ): OwnedFurni
    }

    data class Claim(
        val category: Int,
        val success: Boolean,
        val paid: List<Paid>,
    ) {
        val dailyGifts: Int get() = paid.count { it.reward.category == DAILY_GIFT }
    }

    data class DailyGift(
        val added: Boolean,
        val firstStatusToday: Boolean,
    ) {
        companion object {
            val NONE = DailyGift(added = false, firstStatusToday = false)
        }
    }

    data class Listing(
        val rewards: List<Reward>,
        val firstStatusToday: Boolean,
        val giftAdded: Boolean = false,
    ) {
        val listsDailyGift: Boolean get() = rewards.any { it.category == DAILY_GIFT }
    }

    /** Every call may throw [SQLException]. */
    interface Store {
        fun insert(
            userId: Int,
            category: Int,
            type: Int,
            amount: Int,
            productCode: String,
            created: Int,
        )

        fun list(userId: Int): List<Reward>

        fun grantDailyGift(
            userId: Int,
            day: Int,
            amount: Int,
            created: Int,
        ): DailyGift

        /** A furni reward's row goes only with its furni made. */
        fun pay(
            userId: Int,
            rewards: List<Reward>,
            furni: FurniMaker,
        ): List<Paid>
    }

    /** How many statuses of a session came after UnseenItems(10, [1]). */
    private val giftAnnounced: MutableMap<ClientConnection, Int> = Collections.synchronizedMap(WeakHashMap())

    fun add(
        userId: Int,
        category: Int,
        type: Int,
        amount: Int,
        productCode: String,
        notice: Notice = Notice.NEW_EARNING,
    ): Boolean {
        if (userId <= 0 || category !in 0..MAX_CATEGORY) return false
        if (productCode.isEmpty() && amount <= 0) return false

        try {
            store.insert(userId, category, type, amount, productCode, now())
        } catch (
            @Suppress("TooGenericExceptionCaught") exception: Exception, // a failed store skips this reward only
        ) {
            logger.error("Income reward for user {} (category {}) could not be stored", userId, category, exception)
            return false
        }
        announce(userId, category, notice)
        return true
    }

    fun notifyNew(
        userId: Int,
        category: Int,
    ) {
        if (userId <= 0 || category !in 0..MAX_CATEGORY) return
        announce(userId, category, Notice.NEW_EARNING)
    }

    fun listing(
        userId: Int,
        clubMember: Boolean,
    ): Listing {
        var daily = DailyGift.NONE
        try {
            if (rules.dailyGift) daily = store.grantDailyGift(userId, today(), dailyGiftPoints(clubMember), now())
        } catch (exception: SQLException) {
            logger.error("Daily gift for user {} could not be stored", userId, exception)
        }
        return try {
            Listing(store.list(userId), daily.firstStatusToday, daily.added)
        } catch (exception: SQLException) {
            logger.error("Income rewards of user {} could not be loaded", userId, exception)
            Listing(emptyList(), daily.firstStatusToday, daily.added)
        }
    }

    fun sendStatus(player: Player?) {
        val client = player?.client ?: return
        val listing = listing(player.profile.id, clubMember(player))
        val announced = giftAnnounced[client] ?: 0
        if (sendsUnseenEarning(listing) || announcesAgain(listing, announced)) {
            giftAnnounced[client] = announced + 1
            client.send(unseenEarning())
        }
        client.send(IncomeRewardStatus(listing.rewards))
    }

    fun claim(
        userId: Int,
        category: Int,
    ): Claim {
        val pending =
            try {
                select(store.list(userId), category)
            } catch (exception: SQLException) {
                logger.error("Income rewards of user {} could not be loaded for a claim", userId, exception)
                return Claim(category, false, emptyList())
            }
        val payable = pending.filter { !it.isFurni || furniItem(userId, it) != null }
        if (payable.isEmpty()) return Claim(category, pending.isEmpty(), emptyList())

        return try {
            Claim(
                category,
                true,
                store.pay(userId, payable) { connection, reward -> makeFurni(connection, userId, reward) },
            )
        } catch (
            @Suppress("TooGenericExceptionCaught") exception: Exception, // a failed payout answers a failed claim
        ) {
            logger.error("Income rewards of user {} (category {}) could not be paid", userId, category, exception)
            Claim(category, false, emptyList())
        }
    }

    @Suppress("TooGenericExceptionCaught") // a failed claim answers failure and never reaches the packet handler
    fun claim(
        player: Player?,
        category: Int,
    ) {
        val client = player?.client ?: return
        val userId = player.profile.id

        val claim =
            try {
                WalletWrites.locked(player) {
                    claim(userId, category).also { result ->
                        result.paid
                            .filter { !it.reward.isFurni && it.balanceAfter >= 0 }
                            .forEach {
                                WalletWrites.publish(
                                    player,
                                    it.reward.currency,
                                    it.balanceAfter,
                                )
                            }
                    }
                }
            } catch (exception: RuntimeException) {
                logger.error("Income reward claim of user {} failed", userId, exception)
                Claim(category, false, emptyList())
            }

        client.send(IncomeRewardClaimResponseOutCodec, IncomeRewardClaimResponseOut(category.toByte(), claim.success))

        for (paid in claim.paid) {
            val reward = paid.reward
            when {
                paid.furni != null -> {
                    giveFurni(player, paid.furni)
                }

                paid.balanceAfter < 0 -> {}

                reward.type == CREDITS -> {
                    client.send(UserCredits(paid.balanceAfter))
                }

                else -> {
                    client.send(
                        ActivityPointNotificationOutCodec,
                        ActivityPointNotificationOut(paid.balanceAfter, reward.amount, 0),
                    )
                }
            }
        }
        AchievementEvents.identity().giftsClaimed(player, claim.dailyGifts)
    }

    /** Null (logged) keeps the reward row waiting until the definition exists. */
    private fun furniItem(
        userId: Int,
        reward: Reward,
    ): Item? =
        items()?.getItem(reward.productCode).also {
            if (it ==
                null
            ) {
                logger.warn("Income reward furni '{}' of user {} has no definition", reward.productCode, userId)
            }
        }

    private fun makeFurni(
        connection: Connection,
        userId: Int,
        reward: Reward,
    ): OwnedFurni {
        val loaded = items()
        return loaded?.newFurni(connection, userId, loaded.getItem(reward.productCode), 0, 0, "")
            ?: throw SQLException("income reward furni ${reward.productCode} could not be made")
    }

    private fun giveFurni(
        player: Player,
        item: OwnedFurni,
    ) {
        player.inventory.furni.put(item)
        player.client?.send(AddRoomItem(item))
        player.client?.send(InventoryUpdateItem(item))
    }

    fun announce(
        userId: Int,
        category: Int,
        notice: Notice,
    ) {
        val client = online(userId)?.client ?: return
        if (notice ==
            Notice.NEW_EARNING
        ) {
            client.send(IncomeRewardNotificationOutCodec, IncomeRewardNotificationOut(category.toByte()))
        }
        client.send(unseenEarning())
    }

    private fun now(): Int = (clock.millis() / MILLIS_PER_SECOND).toInt()

    private fun today(): Int {
        val date = LocalDate.now(clock)
        return date.year * YEAR_FACTOR + date.monthValue * MONTH_FACTOR + date.dayOfMonth
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IncomeRewards::class.java)
        private const val YEAR_FACTOR = 10_000
        private const val MONTH_FACTOR = 100

        const val ALL = -1
        const val DAILY_GIFT = 1
        const val ACHIEVEMENTS = 2

        const val MARKETPLACE = 3

        const val CLUB = 4

        const val LEVEL_PROGRESSION = 5

        const val SNOWBALL = 10

        const val GAMES = 11

        const val WIRED_CHEST = 12

        const val AGENCY = 13

        const val MAX_CATEGORY = 13
        const val POINTS = 0
        const val CREDITS = 1

        const val DAILY_GIFT_POINTS = 5

        /** Twice as many as a non-member gets. */
        fun clubPoints(
            points: Int,
            clubMember: Boolean,
        ): Int = if (clubMember) Math.multiplyExact(points, 2) else points

        fun dailyGiftPoints(clubMember: Boolean): Int = clubPoints(DAILY_GIFT_POINTS, clubMember)

        @Throws(SQLException::class)
        fun insert(
            connection: Connection,
            userId: Int,
            category: Int,
            type: Int,
            amount: Int,
            productCode: String,
            created: Int,
        ) = JdbcIncomeRewardStore.insert(connection, userId, category, type, amount, productCode, created)

        fun addsDailyGift(
            lastGiftDay: Int,
            day: Int,
            giftWaiting: Boolean,
        ): Boolean = lastGiftDay < day && !giftWaiting

        fun sendsUnseenEarning(listing: Listing): Boolean =
            listing.listsDailyGift && (listing.firstStatusToday || listing.giftAdded)

        fun announcesAgain(
            listing: Listing,
            announcedBefore: Int,
        ): Boolean = listing.listsDailyGift && announcedBefore == 1

        fun clubMember(player: Player): Boolean = player.stats.club.isMember == true

        fun select(
            rewards: List<Reward>,
            category: Int,
        ): List<Reward> = if (category == ALL) rewards.toList() else rewards.filter { it.category == category }

        fun unseenEarning(): AddRoomItem = AddRoomItem(1, UnseenCategory.EARNINGS)
    }
}
