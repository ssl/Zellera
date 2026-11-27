package zel.game.guilds

import zel.db.LikePattern
import zel.db.Sql
import zel.db.allOrNothing
import zel.db.first
import zel.db.orFallback
import zel.db.write
import zel.db.writeEach
import java.sql.Connection
import javax.sql.DataSource

class GuildMemberStore(
    private val dataSource: DataSource,
) {
    private val queries = Sql(dataSource)

    fun find(
        guildId: Int,
        userId: Int,
    ): Membership? =
        guildStorageLog.orFallback("guild storage: find", null) {
            queries
                .select(SELECT_ACTIVE_MEMBER) {
                    int(guildId)
                    int(userId)
                    int(MemberLevel.BARRED.code)
                }.first { rows -> Membership.of(rows) }
        }

    fun all(guildId: Int): Set<Membership> =
        dataSource.readInto("members of guild $guildId", HashSet(), SELECT_ALL, { int(guildId) }) { Membership.of(it) }

    fun page(
        guildId: Int,
        page: Int,
        standing: Int,
        query: String,
    ): List<Membership> =
        dataSource.readInto(
            "member page $page of guild $guildId",
            ArrayList(),
            "$SELECT_MEMBERS WHERE ${listFilter(standing)} ORDER BY m.level, m.member_since LIMIT ?, ?",
            {
                int(guildId)
                string(likePattern(query))
                int(page * PAGE_SIZE)
                int(PAGE_SIZE)
            },
        ) { Membership.of(it) }

    fun count(
        guildId: Int,
        standing: Int,
        query: String,
    ): Int =
        guildStorageLog.orFallback("guild storage: count", 0) {
            dataSource.connection.use { connection ->
                connection.countOf("SELECT COUNT(*)$MEMBERS_WITH_NAMES WHERE ${listFilter(standing)}") {
                    int(guildId)
                    string(likePattern(query))
                }
            }
        }

    fun isBlocked(
        guildId: Int,
        userId: Int,
    ): Boolean =
        guildStorageLog.orFallback("guild storage: is blocked", false) {
            queries
                .select(SELECT_BLOCKED) {
                    int(guildId)
                    int(userId)
                    int(MemberLevel.BARRED.code)
                }.result { it.next() }
        }

    fun unblock(
        guildId: Int,
        userId: Int,
    ): Boolean =
        guildStorageLog.orFallback("guild storage: unblock", false) {
            dataSource.connection.use { connection ->
                connection.write(DELETE_BLOCKED) {
                    int(userId)
                    int(guildId)
                    int(MemberLevel.BARRED.code)
                } > 0
            }
        }

    fun setRank(
        guildId: Int,
        userId: Int,
        rank: MemberLevel,
    ) {
        guildStorageLog.orFallback("guild storage: set rank", Unit) {
            dataSource.connection.use { connection ->
                connection.write(SET_RANK) {
                    int(rank.code)
                    int(userId)
                    int(guildId)
                }
            }
        }
    }

    fun delete(
        guildId: Int,
        userId: Int,
    ) {
        guildStorageLog.orFallback("guild storage: delete member", Unit) {
            dataSource.connection.use { connection ->
                connection.write(DELETE_MEMBER) {
                    int(userId)
                    int(guildId)
                }
            }
        }
    }

    fun clearFavourite(
        userId: Int,
        guildId: Int,
    ) {
        guildStorageLog.orFallback("guild storage: clear favourite", Unit) {
            dataSource.connection.use { connection ->
                connection.write(CLEAR_FAVOURITE) {
                    int(userId)
                    int(guildId)
                }
            }
        }
    }

    fun <T : Any> inTransaction(
        fallback: T,
        work: (Connection) -> T,
    ): T =
        guildStorageLog.orFallback("guild storage: member change", fallback) {
            dataSource.connection.use { connection -> connection.allOrNothing { work(connection) } } ?: fallback
        }

    /** Locks the guild's row until the transaction ends, so member counts read under it stay true. */
    fun lockGuild(
        connection: Connection,
        guildId: Int,
    ) {
        connection.first(LOCK_GUILD, { int(guildId) }) { true }
    }

    /** Groups the user is in or has applied to; an application holds its place so acceptance never passes the limit. */
    fun groupsOf(
        connection: Connection,
        userId: Int,
    ): Int =
        connection.countOf(COUNT_GROUPS_OF_USER) {
            int(userId)
            int(MemberLevel.BARRED.code)
        }

    fun activeCount(
        connection: Connection,
        guildId: Int,
    ): Int =
        connection.countOf(COUNT_ACTIVE) {
            int(guildId)
            int(MemberLevel.APPLICANT.code)
        }

    fun hasRow(
        connection: Connection,
        guildId: Int,
        userId: Int,
    ): Boolean =
        connection.countOf(COUNT_ROW) {
            int(guildId)
            int(userId)
        } >= 1

    fun insertMember(
        connection: Connection,
        guildId: Int,
        userId: Int,
        since: Int,
        rank: MemberLevel,
    ) {
        connection.write(INSERT_MEMBER) {
            int(guildId)
            int(userId)
            int(rank.code)
            int(since)
        }
    }

    fun acceptRequest(
        connection: Connection,
        guildId: Int,
        userId: Int,
        since: Int,
    ): Int =
        connection.write(ACCEPT_REQUEST) {
            int(MemberLevel.REGULAR.code)
            int(since)
            int(userId)
            int(guildId)
            int(MemberLevel.APPLICANT.code)
        }

    fun acceptRequests(
        connection: Connection,
        guildId: Int,
        userIds: List<Int>,
        since: Int,
    ): IntArray =
        connection.writeEach(ACCEPT_REQUEST, userIds) { userId ->
            int(MemberLevel.REGULAR.code)
            int(since)
            int(userId)
            int(guildId)
            int(MemberLevel.APPLICANT.code)
        }

    private fun likePattern(query: String) = LikePattern.containing(query)

    private fun listFilter(standing: Int): String {
        val levels =
            when (standing) {
                STANDING_BLOCKED -> listOf(MemberLevel.BARRED)
                STANDING_REQUESTS -> listOf(MemberLevel.APPLICANT)
                STANDING_ADMINS -> listOf(MemberLevel.FOUNDER, MemberLevel.OFFICER)
                else -> listOf(MemberLevel.FOUNDER, MemberLevel.OFFICER, MemberLevel.REGULAR)
            }
        return "m.guild_id = ? AND m.level IN (${levels.joinToString { it.code.toString() }}) AND p.name LIKE ?"
    }

    private companion object {
        const val PAGE_SIZE = 14
        const val STANDING_ADMINS = 1
        const val STANDING_REQUESTS = 2
        const val STANDING_BLOCKED = 3

        const val MEMBERS_WITH_NAMES = " FROM guild_members m JOIN players p ON p.id = m.player_id"
        const val SELECT_MEMBERS =
            "SELECT m.guild_id, m.player_id, m.level, m.member_since, p.name, p.figure$MEMBERS_WITH_NAMES"
        const val SELECT_ACTIVE_MEMBER =
            "$SELECT_MEMBERS WHERE m.guild_id = ? AND m.player_id = ? AND m.level <> ?"
        const val SELECT_ALL = "$SELECT_MEMBERS WHERE m.guild_id = ?"
        const val SELECT_BLOCKED = "SELECT 1 FROM guild_members WHERE guild_id = ? AND player_id = ? AND level = ?"
        const val DELETE_BLOCKED = "DELETE FROM guild_members WHERE player_id = ? AND guild_id = ? AND level = ?"
        const val SET_RANK = "UPDATE guild_members SET level = ? WHERE player_id = ? AND guild_id = ?"
        const val DELETE_MEMBER = "DELETE FROM guild_members WHERE player_id = ? AND guild_id = ?"
        const val CLEAR_FAVOURITE =
            "UPDATE player_stats SET favourite_guild_id = NULL WHERE player_id = ? AND favourite_guild_id = ?"
        const val COUNT_GROUPS_OF_USER = "SELECT COUNT(*) FROM guild_members WHERE player_id = ? AND level <> ?"
        const val COUNT_ACTIVE = "SELECT COUNT(*) FROM guild_members WHERE guild_id = ? AND level < ?"
        const val COUNT_ROW = "SELECT COUNT(*) FROM guild_members WHERE guild_id = ? AND player_id = ?"
        const val INSERT_MEMBER =
            "INSERT INTO guild_members (guild_id, player_id, level, member_since) VALUES (?, ?, ?, ?)"
        const val LOCK_GUILD = "SELECT id FROM guilds WHERE id = ? FOR UPDATE"

        /** Promotes an applicant to a member; a row that is not an applicant any more stays as it is. */
        const val ACCEPT_REQUEST =
            "UPDATE guild_members SET level = ?, member_since = ? WHERE player_id = ? AND guild_id = ? AND level = ?"
    }
}
