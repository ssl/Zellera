package zel.game.guilds

import zel.core.rules.rules
import zel.game.players.Player

class GuildMemberships(
    private val rows: GuildMemberStore,
    private val clock: () -> Int,
) {
    fun find(
        guild: Guild,
        player: Player,
    ): Membership? = rows.find(guild.id, player.profile.id)

    fun find(
        guildId: Int,
        userId: Int,
    ): Membership? = rows.find(guildId, userId)

    /** An open group takes the player in at once, an exclusive one records a request; null when nothing refused it. */
    fun request(
        guild: Guild,
        player: Player,
    ): GuildJoinFailure? {
        val userId = player.profile.id
        if (rows.isBlocked(guild.id, userId)) return GuildJoinFailure.CLOSED
        val requesting = guild.state.needsApproval
        val outcome =
            rows.inTransaction<Outcome>(Outcome.Failed) { connection ->
                rows.lockGuild(connection, guild.id)
                when {
                    rows.hasRow(connection, guild.id, userId) -> {
                        Outcome.AlreadyThere
                    }

                    rows.groupsOf(connection, userId) >= membershipLimit(player) -> {
                        Outcome.Refused(GuildJoinFailure.TOO_MANY_GUILDS)
                    }

                    !requesting && rows.activeCount(connection, guild.id) >= rules.groupMaxMembers -> {
                        Outcome.Refused(GuildJoinFailure.FULL)
                    }

                    else -> {
                        val rank = if (requesting) MemberLevel.APPLICANT else MemberLevel.REGULAR
                        rows.insertMember(connection, guild.id, userId, clock(), rank)
                        Outcome.Added(rank)
                    }
                }
            }
        return when (outcome) {
            is Outcome.Added -> {
                if (outcome.rank == MemberLevel.APPLICANT) {
                    guild.requestMade()
                } else {
                    guild.memberJoined()
                    player.stats.addGuild(guild.id)
                }
                null
            }

            is Outcome.Refused -> {
                outcome.reason
            }

            Outcome.AlreadyThere, Outcome.Failed -> {
                null
            }
        }
    }

    fun accept(
        guild: Guild,
        userId: Int,
    ): Acceptance =
        rows.inTransaction(Acceptance.Failed) { connection ->
            rows.lockGuild(connection, guild.id)
            when {
                rows.activeCount(connection, guild.id) >= rules.groupMaxMembers -> {
                    Acceptance.Refused(GuildJoinFailure.FULL)
                }

                rows.acceptRequest(connection, guild.id, userId, clock()) == 1 -> {
                    Acceptance.Accepted
                }

                else -> {
                    Acceptance.Failed
                }
            }
        }

    /** Accepts [userIds]' requests in order in one transaction while places remain; the rest are refused as full. */
    fun acceptAll(
        guild: Guild,
        userIds: List<Int>,
    ): List<Acceptance> =
        rows.inTransaction(userIds.map { Acceptance.Failed }) { connection ->
            rows.lockGuild(connection, guild.id)
            val places = maxOf(0, rules.groupMaxMembers - rows.activeCount(connection, guild.id))
            val chosen = userIds.take(places)
            val changed = rows.acceptRequests(connection, guild.id, chosen, clock())
            userIds.indices.map { index ->
                when {
                    index >= chosen.size -> Acceptance.Refused(GuildJoinFailure.FULL)
                    changed[index] == 1 -> Acceptance.Accepted
                    else -> Acceptance.Failed
                }
            }
        }

    sealed interface Acceptance {
        data object Accepted : Acceptance

        data class Refused(
            val reason: GuildJoinFailure,
        ) : Acceptance

        data object Failed : Acceptance
    }

    fun remove(
        guild: Guild,
        userId: Int,
    ) = rows.delete(guild.id, userId)

    fun clearFavourite(
        userId: Int,
        guildId: Int,
    ) = rows.clearFavourite(userId, guildId)

    fun makeAdmin(
        guild: Guild,
        userId: Int,
    ) {
        if (rows.find(guild.id, userId)?.role == MemberLevel.REGULAR) {
            rows.setRank(guild.id, userId, MemberLevel.OFFICER)
        }
    }

    fun dropAdmin(
        guild: Guild,
        userId: Int,
    ) {
        if (rows.find(guild.id, userId)?.role == MemberLevel.OFFICER) {
            rows.setRank(guild.id, userId, MemberLevel.REGULAR)
        }
    }

    fun unblock(
        guild: Guild,
        userId: Int,
    ) = rows.unblock(guild.id, userId)

    /** [level] is the wire search type: all, admins, requests, blocked. */
    fun page(
        guild: Guild,
        page: Int,
        level: Int,
        query: String,
    ): List<Membership> = rows.page(guild.id, page, level, query)

    fun count(
        guild: Guild,
        level: Int,
        query: String,
    ): Int = rows.count(guild.id, level, query)

    fun atMembershipLimit(player: Player): Boolean =
        rows.inTransaction(false) { connection ->
            rows.groupsOf(connection, player.profile.id) >= membershipLimit(player)
        }

    private fun membershipLimit(player: Player): Int =
        if (player.stats.club.isMember) rules.groupMaxMembershipsClub else rules.groupMaxMemberships

    private sealed interface Outcome {
        data class Added(
            val rank: MemberLevel,
        ) : Outcome

        data class Refused(
            val reason: GuildJoinFailure,
        ) : Outcome

        data object AlreadyThere : Outcome

        data object Failed : Outcome
    }
}
