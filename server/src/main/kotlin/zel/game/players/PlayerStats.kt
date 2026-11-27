package zel.game.players

import org.slf4j.LoggerFactory
import zel.db.SqlFailure
import zel.game.achievements.Achievement
import zel.game.messenger.MessengerPace
import zel.game.permissions.PlayerLimits
import zel.server.GameServices
import java.util.concurrent.CopyOnWriteArrayList

private val log = LoggerFactory.getLogger(PlayerStats::class.java)

class PlayerStats internal constructor(
    private val services: GameServices,
    profile: PlayerProfile,
) : Runnable {
    private val store = PlayerStatsStore(services, profile.id)

    val tally = PlayerTally(store)
    val settings = PlayerSettings(store)
    val standing = PlayerStanding(services::unixTimestamp)
    val contacts = PlayerContacts(profile.id, store)
    val club = ClubMembership(services, profile.id, this) { profile.ledgerMutationLock }
    val limitedPurchases = LimitedPurchaseLog(services::unixTimestamp)
    val navigatorLayouts = NavigatorLayouts(services, profile.id)
    val favouriteRooms = FavouriteRooms(store) { PlayerLimits.maxFavouriteRooms(profile.rank) }
    val recipes = RecipeBook(store)

    /** Read on room, wired and group threads without the stats lock. */
    val guildIds: MutableList<Int> = CopyOnWriteArrayList()

    @Volatile
    var favouriteGuild = 0
    val achievementProgress: MutableMap<Achievement, Int> = HashMap()
    internal val offerPurchases = HashMap<Int, PlayerOfferPurchase>()

    var rentedSpaceId = 0
    var rentedUntil = 0

    val sessionStartedAt: Int = services.unixTimestamp()
    val messengerPace = MessengerPace()
    var isPurchasingFurniture = false
    var lastPurchaseTimestamp = 0L
    var clubLastChangedAt = sessionStartedAt

    private var onlineCountedUntil = sessionStartedAt

    val isRentingSpace: Boolean get() = rentedUntil >= services.unixTimestamp()

    @Synchronized
    override fun run() {
        store.requireOpen()
        val now = services.unixTimestamp()
        try {
            store.saveSnapshot(this, now - onlineCountedUntil)
            onlineCountedUntil = now
            store.saveOfferPurchases(synchronized(offerPurchases) { offerPurchases.values.toList() })
        } catch (e: SqlFailure) {
            log.error("The stats of a player could not be saved", e)
        }
    }

    fun storeMute() = store.storeMutedUntil(standing.mutedUntil)

    @Synchronized
    fun dispose() {
        run()
        store.close()
    }

    @Synchronized
    fun addGuild(guildId: Int) {
        if (guildId !in guildIds) guildIds += guildId
    }

    @Synchronized
    fun removeGuild(guildId: Int) {
        guildIds.remove(guildId)
        if (favouriteGuild == guildId) favouriteGuild = 0
    }

    fun progressOf(achievement: Achievement?): Int =
        achievement?.let { synchronized(achievementProgress) { achievementProgress[it] } } ?: 0

    fun recordProgress(
        achievement: Achievement,
        progress: Int,
    ) = synchronized(achievementProgress) { achievementProgress[achievement] = progress }

    fun offerPurchaseOrNull(offerId: Int): PlayerOfferPurchase? =
        synchronized(offerPurchases) { offerPurchases[offerId] }

    fun offerPurchase(offerId: Int): PlayerOfferPurchase? =
        synchronized(offerPurchases) {
            offerPurchases[offerId] ?: try {
                store.startOfferPurchase(offerId)
                PlayerOfferPurchase(offerId).also { offerPurchases[offerId] = it }
            } catch (e: SqlFailure) {
                log.error("The offer purchase {} could not be started", offerId, e)
                null
            }
        }

    companion object {
        fun load(
            services: GameServices,
            playerProfile: PlayerProfile,
        ): PlayerStats = PlayerStatsLoader(services, playerProfile).load()
    }
}
