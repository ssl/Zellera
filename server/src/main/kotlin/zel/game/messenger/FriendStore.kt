package zel.game.messenger

import zel.db.Sql
import zel.db.allOrNothing
import zel.db.orFallback
import zel.db.write
import zel.game.players.Gender
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import javax.sql.DataSource

class FriendStore(
    private val dataSource: DataSource,
    private val sql: Sql,
    private val connected: ConnectedPlayers,
    private val clock: () -> Int,
) {
    fun friendsOf(ownerId: Int): List<Friend> =
        LOGGER.orFallback("friend list of $ownerId", emptyList()) {
            sql.select(LIST, ownerId).rows { listedFriend(ownerId, it) }
        }

    fun friendOf(
        ownerId: Int,
        friendId: Int,
    ): Friend? =
        LOGGER.orFallback("friend $friendId of $ownerId", null) {
            sql.select(ONE, ownerId, friendId).first { listedFriend(ownerId, it) }
        }

    fun countOf(playerId: Int): Int =
        connected
            .byId(playerId)
            ?.messenger
            ?.friends
            ?.size
            ?: LOGGER.orFallback("friend count of $playerId", 0) {
                sql.select(COUNT, playerId).first { it.getInt(1) } ?: 0
            }

    /** Per relation (1 heart, 2 smile, 3 skull) how many friends carry it and one of them, picked at random. */
    fun relationSummary(playerId: Int): List<RelationSummary> =
        LOGGER.orFallback("relations of $playerId", emptyList()) {
            sql.select(RELATIONS, playerId).rows {
                RelationSummary(
                    relation = it.getInt("relation"),
                    friends = it.getInt("total"),
                    shownId = it.getInt("friend_id"),
                    shownName = it.getString("name"),
                    shownLook = it.getString("figure"),
                )
            }
        }

    /** Makes [senderId] and [targetId] friends if [senderId] asked [targetId]; a request the other way goes too. */
    fun acceptRequest(
        senderId: Int,
        targetId: Int,
    ): Boolean =
        LOGGER.orFallback("accepting the friend request of player $senderId to $targetId", false) {
            dataSource.connection.use { connection ->
                connection.allOrNothing {
                    val asked =
                        connection.write(FriendRequestStore.TAKE) {
                            int(senderId)
                            int(targetId)
                        } > 0
                    if (!asked) return@allOrNothing null
                    connection.write(FriendRequestStore.TAKE) {
                        int(targetId)
                        int(senderId)
                    }
                    val since = clock()
                    connection.write(BEFRIEND) {
                        int(senderId)
                        int(targetId)
                        int(since)
                        int(targetId)
                        int(senderId)
                        int(since)
                    }
                    true
                }
            } == true
        }

    fun unfriend(
        first: Int,
        second: Int,
    ) = LOGGER.orFallback("ending the friendship of $first and $second", Unit) {
        sql.write(UNFRIEND, first, second, second, first)
    }

    fun saveRelation(friend: Friend) =
        LOGGER.orFallback("relation of ${friend.ownerId} to ${friend.id}", Unit) {
            sql.write(SET_RELATION, friend.relation.toInt(), friend.ownerId, friend.id)
        }

    private fun listedFriend(
        ownerId: Int,
        row: ResultSet,
    ): Friend {
        val username = row.getString("name")
        val seenOnline = row.getBoolean("is_online")
        return Friend(
            id = row.getInt("friend_id"),
            ownerId = ownerId,
            username = username,
            look = row.getString("figure"),
            connected = connected,
            relation = row.getInt("relation").toShort(),
            gender = enumValueOf<Gender>(row.getString("gender")),
            motto = row.getString("motto"),
            categoryId = row.getInt("category_id"),
            lastKnownOnline = seenOnline,
            lastKnownInRoom = seenOnline && connected.byName(username)?.inRoom == true,
        )
    }

    internal companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(FriendStore::class.java)
        private const val LIST =
            "SELECT f.friend_id, f.relation, f.category_id, p.name, p.figure, p.gender, p.motto, p.is_online " +
                "FROM friendships f INNER JOIN players p ON p.id = f.friend_id WHERE f.player_id = ?"
        private const val ONE = "$LIST AND f.friend_id = ?"
        private const val COUNT = "SELECT COUNT(*) FROM friendships WHERE player_id = ?"
        private const val RELATIONS =
            "SELECT s.relation, s.total, s.friend_id, p.name, p.figure FROM (" +
                "SELECT f.relation, f.friend_id, COUNT(*) OVER (PARTITION BY f.relation) AS total, " +
                "ROW_NUMBER() OVER (PARTITION BY f.relation ORDER BY RAND()) AS pick " +
                "FROM friendships f WHERE f.player_id = ? AND f.relation > 0) s " +
                "INNER JOIN players p ON p.id = s.friend_id WHERE s.pick = 1 ORDER BY s.relation"

        /** Both sides in one statement; a side that exists keeps its date. */
        const val BEFRIEND =
            "INSERT INTO friendships (player_id, friend_id, friends_since) VALUES (?, ?, ?), (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE friends_since = friendships.friends_since"
        private const val UNFRIEND = "DELETE FROM friendships WHERE (player_id, friend_id) IN ((?, ?), (?, ?))"
        private const val SET_RELATION = "UPDATE friendships SET relation = ? WHERE player_id = ? AND friend_id = ?"
    }
}
