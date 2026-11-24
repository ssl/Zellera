package zel.game.guilds

import zel.db.Sql
import zel.db.allOrNothing
import zel.db.insertForId
import zel.db.orFallback
import zel.db.write
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource

class GuildStore(
    private val dataSource: DataSource,
    private val clock: () -> Int,
) {
    private val queries = Sql(dataSource)

    fun insert(
        ownerId: Int,
        ownerName: String,
        details: NewGuild,
    ): Guild? {
        val now = clock()
        val crest = details.crest
        val face = Guild.Face(details.name, details.about, crest.code, crest.primary, crest.secondary)
        val id =
            guildStorageLog.orFallback("guild storage: insert guild of player $ownerId", null) {
                dataSource.connection.use { connection ->
                    connection.allOrNothing {
                        connection.insertGroupRow(ownerId, details.home.id, face, now)?.takeIf { id ->
                            connection.write(INSERT_OWNER) {
                                int(id)
                                int(ownerId)
                                int(MemberLevel.FOUNDER.code)
                                int(now)
                            } > 0
                        }
                    }
                }
            } ?: return null
        return Guild(
            id,
            ownerId,
            ownerName,
            details.home.id,
            details.home.title,
            now,
            face,
            Guild.Joining(JoinMode.WALK_IN, membersDecorate = false),
            ForumRules(),
            hasForum = false,
        ).also { it.memberJoined() }
    }

    private fun Connection.insertGroupRow(
        ownerId: Int,
        roomId: Int,
        face: Guild.Face,
        now: Int,
    ): Int? =
        insertForId(INSERT_GUILD) {
            string(face.name)
            string(face.description)
            intOrNull(roomId)
            int(ownerId)
            int(face.colorOne)
            int(face.colorTwo)
            string(face.badge)
            int(now)
        }

    fun save(guild: Guild) {
        if (!guild.hasUnsavedChanges) return
        guildStorageLog.orFallback("guild storage: save", Unit) {
            val rules = guild.forumRules
            dataSource.connection.use { connection ->
                connection.write(UPDATE_GUILD) {
                    string(guild.name)
                    string(guild.description)
                    int(guild.state.code)
                    boolean(guild.rights)
                    int(guild.colorOne)
                    int(guild.colorTwo)
                    string(guild.badge)
                    string(rules.read.name)
                    string(rules.postMessages.name)
                    string(rules.startThreads.name)
                    string(rules.moderate.name)
                    boolean(guild.hasForum)
                    int(guild.id)
                }
            }
            guild.saved()
        }
    }

    fun load(guildId: Int): Guild? {
        val guild =
            guildStorageLog.orFallback("guild storage: load", null) {
                queries.select(SELECT_GUILD) { int(guildId) }.first { rows -> rows.toGuild() }
            } ?: return null
        readHeadcount(guild)
        return guild
    }

    private fun ResultSet.toGuild(): Guild =
        Guild(
            getInt("id"),
            getInt("owner_id"),
            getString("owner_name").orEmpty(),
            getInt("room_id"),
            getString("room_name").orEmpty(),
            getInt("created_at"),
            Guild.Face(
                getString("name").orEmpty(),
                getString("description").orEmpty(),
                getString("badge_code").orEmpty(),
                getInt("color_one"),
                getInt("color_two"),
            ),
            Guild.Joining(JoinMode.fromStored(getInt("join_mode")), getBoolean("members_may_decorate")),
            ForumRules(
                ForumLevel.valueOf(getString("forum_read")),
                ForumLevel.valueOf(getString("forum_post")),
                ForumLevel.valueOf(getString("forum_new_thread")),
                ForumLevel.valueOf(getString("forum_moderate")),
            ),
            getBoolean("has_forum"),
        )

    private fun readHeadcount(guild: Guild) {
        guildStorageLog.orFallback("guild storage: read headcount", Unit) {
            dataSource.connection.use { connection ->
                guild.countMembers(
                    connection.countOf(COUNT_MEMBERS) {
                        int(MemberLevel.REGULAR.code)
                        int(guild.id)
                    },
                )
                guild.countRequests(
                    connection.countOf(COUNT_REQUESTS) {
                        int(MemberLevel.APPLICANT.code)
                        int(guild.id)
                    },
                )
            }
        }
    }

    fun idsOfMember(userId: Int): List<Int> =
        dataSource.readInto(
            "guilds of player $userId",
            ArrayList(),
            SELECT_MEMBER_OF,
            {
                int(userId)
                int(MemberLevel.REGULAR.code)
            },
        ) { it.getInt("guild_id") }

    fun idsOwnedBy(userId: Int): List<Int> =
        dataSource.readInto("guilds owned by player $userId", ArrayList(), SELECT_OWNED_BY, { int(userId) }) {
            it.getInt("id")
        }

    /** One statement: members, forum, visits and reads go with the guild, rooms and furni lose its tag. */
    fun delete(guildId: Int): Boolean =
        guildStorageLog.orFallback("guild storage: delete guild", false) {
            dataSource.connection.use { connection -> connection.write(DELETE_GUILD) { int(guildId) } }
            true
        }

    /** Tags the furni inside the caller's transaction; a furni row that isn't there fails it. */
    @Throws(SQLException::class)
    fun tagFurni(
        connection: Connection,
        furniId: Int,
        guildId: Int,
    ) {
        val changed =
            connection.write(TAG_FURNI) {
                intOrNull(guildId)
                int(furniId)
            }
        if (changed != 1) throw SQLException("Unable to assign guild furniture $furniId")
    }

    private companion object {
        const val INSERT_GUILD =
            "INSERT INTO guilds (name, description, room_id, owner_id, color_one, color_two, badge_code, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        const val INSERT_OWNER =
            "INSERT INTO guild_members (guild_id, player_id, level, member_since) VALUES (?, ?, ?, ?)"
        const val SELECT_GUILD =
            "SELECT g.id, g.owner_id, o.name AS owner_name, g.room_id, r.name AS room_name, g.created_at, g.name, " +
                "g.description, g.badge_code, g.color_one, g.color_two, g.join_mode, g.members_may_decorate, " +
                "g.forum_read, g.forum_post, g.forum_new_thread, g.forum_moderate, g.has_forum FROM guilds g " +
                "INNER JOIN players o ON o.id = g.owner_id INNER JOIN rooms r ON r.id = g.room_id WHERE g.id = ?"
        const val UPDATE_GUILD =
            "UPDATE guilds SET name = ?, description = ?, join_mode = ?, members_may_decorate = ?, color_one = ?, " +
                "color_two = ?, badge_code = ?, forum_read = ?, forum_post = ?, forum_new_thread = ?, " +
                "forum_moderate = ?, has_forum = ? WHERE id = ?"

        /** Levels up to [MemberLevel.REGULAR] are members, [MemberLevel.APPLICANT] asks to join. */
        const val COUNT_MEMBERS = "SELECT COUNT(*) as count FROM guild_members WHERE level <= ? AND guild_id = ?"
        const val COUNT_REQUESTS = "SELECT COUNT(*) as count FROM guild_members WHERE level = ? AND guild_id = ?"
        const val SELECT_MEMBER_OF =
            "SELECT guild_id FROM guild_members WHERE player_id = ? AND level <= ? ORDER BY member_since ASC"
        const val SELECT_OWNED_BY = "SELECT id FROM guilds WHERE owner_id = ?"
        const val TAG_FURNI = "UPDATE furni SET guild_id = ? WHERE id = ?"
        const val DELETE_GUILD = "DELETE FROM guilds WHERE id = ?"
    }
}
