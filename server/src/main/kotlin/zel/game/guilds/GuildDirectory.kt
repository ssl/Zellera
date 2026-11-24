package zel.game.guilds

import org.slf4j.LoggerFactory
import zel.game.guilds.forums.ForumLists
import zel.game.guilds.forums.ForumPresenter
import zel.game.guilds.forums.ForumReadMarks
import zel.game.guilds.forums.ForumSummaries
import zel.game.guilds.forums.ForumTables
import zel.game.guilds.forums.ForumThreadStore
import zel.game.guilds.forums.ForumVisitLog
import zel.game.players.Player
import zel.game.players.PlayerProfile
import zel.protocol.outgoing.guilds.GuildMessages
import zel.room.port.RoomPort
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

class GuildDirectory(
    dataSource: DataSource,
    clock: () -> Int,
    val messages: GuildMessages,
    private val players: (Int) -> Player?,
    profiles: (Int) -> PlayerProfile?,
    private val rooms: () -> RoomPort,
) {
    private val store = GuildStore(dataSource, clock)
    private val memberRows = GuildMemberStore(dataSource)
    private val cache = GuildCache(clock)

    val members = GuildMemberships(memberRows, clock)
    val parts = GuildPartCatalog(dataSource)
    val forumVisits = ForumVisitLog(dataSource, clock)
    val forumThreads = ForumThreadStore(ForumTables(dataSource), clock)
    val forumPresenter = ForumPresenter(profiles, clock)
    val forumMarks = ForumReadMarks(dataSource)
    val forumLists = ForumLists(dataSource, clock, ::byId)
    val forumSummaries = ForumSummaries(forumThreads, forumMarks, profiles, clock)

    init {
        log.info("Guilds loaded")
    }

    fun byId(guildId: Int): Guild? = cache.getOrLoad(guildId, store::load)

    fun ofMember(userId: Int): List<Guild> = store.idsOfMember(userId).mapNotNull(::byId)

    fun ownedBy(userId: Int): List<Guild> = store.idsOwnedBy(userId).mapNotNull(::byId)

    fun allMembers(guildId: Int): Set<Membership> = memberRows.all(guildId)

    fun remember(guild: Guild) = cache.hold(guild)

    fun save(guild: Guild) = store.save(guild)

    fun create(
        player: Player,
        details: NewGuild,
    ): Guild? {
        val profile = player.profile
        val guild = store.insert(profile.id, profile.name, details) ?: return null
        player.stats.addGuild(guild.id)
        return guild
    }

    /** Deletes the guild; returns the online members whose favourite it was, so their room badge can be cleared. */
    fun delete(guild: Guild): List<Player> {
        val unfavoured = ArrayList<Player>()
        memberRows.all(guild.id).forEach { member ->
            val player = players(member.playerId) ?: return@forEach
            val stats = player.stats
            stats.removeGuild(guild.id)
            if (stats.favouriteGuild == guild.id) {
                stats.favouriteGuild = 0
                unfavoured += player
            }
        }
        if (store.delete(guild.id)) rooms().groupRemoved(guild.roomId)
        cache.remove(guild.id)
        forumThreads.forget(guild.id)
        forumMarks.invalidateUnread(guild.id)
        return unfavoured
    }

    /** Tags the furni inside the caller's transaction. */
    @Throws(SQLException::class)
    fun tagFurni(
        connection: Connection,
        furniId: Int,
        guildId: Int,
    ) = store.tagFurni(connection, furniId, guildId)

    fun mostViewedForums(): Set<Guild> = mostViewedForums(forumVisits.groupsByRecentVisits(), ::byId)

    fun forgetIdle() = cache.forgetIdle()

    fun dispose() {
        cache.flush(store::save)
        log.info("Guilds disposed")
    }

    companion object {
        private const val MOST_VIEWED_FORUMS = 100
        private val log = LoggerFactory.getLogger(GuildDirectory::class.java)

        internal fun mostViewedForums(
            ranked: List<Int>,
            guildById: (Int) -> Guild?,
        ): Set<Guild> =
            ranked
                .asSequence()
                .mapNotNull(guildById)
                .filter { it.forumRules.read == ForumLevel.EVERYONE }
                .take(MOST_VIEWED_FORUMS)
                .toCollection(LinkedHashSet())
    }
}
