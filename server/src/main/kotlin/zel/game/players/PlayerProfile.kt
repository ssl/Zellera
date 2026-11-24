package zel.game.players

import zel.game.economy.EconomyLedger
import zel.game.messenger.FriendCategory
import zel.game.navigation.NavigatorSavedSearch
import zel.game.permissions.Rank
import zel.server.GameServices
import java.sql.ResultSet

class PlayerProfile private constructor(
    private val services: GameServices,
    row: PlayerAccountRow,
    private val store: PlayerProfileStore,
) : Runnable {
    constructor(services: GameServices, set: ResultSet) : this(services, PlayerAccountRow.read(services, set)) {
        store.readBalances(balances)
        searches += store.readSavedSearches()
        categories += store.readFriendCategories()
    }

    private constructor(services: GameServices, row: PlayerAccountRow) :
        this(services, row, PlayerProfileStore(services, row.id))

    internal constructor(services: GameServices, id: Int, credits: Int) :
        this(services, PlayerAccountRow(id = id, credits = Balance.of(credits).amount))

    val id: Int = row.id
    var name: String = row.name
    var motto: String = row.motto
    var figure: String = row.figure
    var gender: Gender = row.gender
    var lastIp: String = row.lastIp
    var rank: Rank = row.rank
    var createdAt: Int = row.createdAt
    var lastOnlineAt: Int = row.lastOnlineAt
    var homeRoomId: Int = row.homeRoomId
    var machineId: String = row.machineId

    @Volatile
    var isOnline: Boolean = false

    /** The settings rows had to be created: the player never logged in before. */
    var firstVisit = false

    /** Serializes the durable wallet mutations of this player with the publication of their result. */
    val ledgerMutationLock = Any()

    private val balances = WalletBalances(row.credits)
    private val searches = ArrayList<NavigatorSavedSearch>()
    private val categories = ArrayList<FriendCategory>()

    val savedSearches: List<NavigatorSavedSearch> get() = synchronized(searches) { searches.toList() }

    val friendCategories: List<FriendCategory> get() = synchronized(categories) { categories.toList() }

    /** The online session's stats, else a fresh load from the database on every call: keep the result. */
    fun loadStats(): PlayerStats = services.onlinePlayer(id)?.stats ?: PlayerStats.load(services, this)

    fun hasSearch(
        searchCode: String?,
        filter: String?,
    ): Boolean = synchronized(searches) { searches.any { it.searchCode == searchCode && it.filter == filter } }

    /** A search takes the lowest number the player has free, so the numbers stay as small as the list. */
    fun saveSearch(search: NavigatorSavedSearch) {
        val numbered =
            synchronized(searches) {
                if (searches.any { it.searchCode == search.searchCode && it.filter == search.filter }) return
                val taken = searches.mapTo(HashSet()) { it.id }
                search.copy(id = generateSequence(1) { it + 1 }.first { it !in taken }).also { searches += it }
            }
        if (!store.addSavedSearch(numbered)) synchronized(searches) { searches -= numbered }
    }

    fun deleteSearch(search: NavigatorSavedSearch) {
        if (synchronized(searches) { searches.removeIf { it.id == search.id } }) store.deleteSavedSearch(search.id)
    }

    val credits: Int get() = balances.credits()

    val points: Int get() = balance(EconomyLedger.POINTS)

    fun balance(type: Int): Int = balances.currency(type)

    /** Takes a balance the ledger committed; writes nothing. */
    fun publishCredits(amount: Int) = balances.replaceCredits(amount)

    fun publishBalance(
        type: Int,
        amount: Int,
    ) = balances.setCurrency(type, amount)

    override fun run() = store.save(this)
}
