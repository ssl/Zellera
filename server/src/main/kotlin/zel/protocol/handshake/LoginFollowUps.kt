package zel.protocol.handshake

import org.slf4j.LoggerFactory
import zel.core.Configuration
import zel.core.threading.GameThreads
import zel.db.Sql
import zel.db.SqlFailure
import zel.db.orFallback
import zel.game.achievements.Achievements
import zel.game.campaign.calendar.CampaignCalendars
import zel.game.catalog.Catalog
import zel.game.moderation.ModerationService
import zel.game.moderation.Sanctions
import zel.game.permissions.applyRankItems
import zel.game.players.Player
import zel.game.players.subscriptions.ClubReactions
import zel.net.clients.ClientConnection
import zel.protocol.generated.AccountSafetyLockStatusChangeOut
import zel.protocol.generated.AccountSafetyLockStatusChangeOutCodec
import zel.protocol.generated.InClientLinkOut
import zel.protocol.generated.InClientLinkOutCodec
import zel.protocol.generated.NotificationDialogOutCodec
import zel.protocol.generated.TargetedOfferOutCodec
import zel.protocol.navigator.NewNavigatorSavedSearches
import zel.protocol.outgoing.catalog.CatalogMessages
import zel.protocol.outgoing.inventory.BadgePointLimits
import zel.wired.chest.ChestNotice
import zel.wired.chest.ChestNotices

class LoginFollowUps(
    private val config: Configuration,
    private val game: Services,
    private val queries: Sql,
    private val now: () -> Int,
) {
    class Services(
        val threading: GameThreads,
        val sanctions: Sanctions,
        val moderation: ModerationService,
        val achievements: Achievements,
        val loginAchievements: LoginAchievements,
        val calendars: CampaignCalendars,
        val catalog: Catalog,
    )

    fun run(
        client: ClientConnection,
        player: Player,
    ) {
        val id = player.profile.id
        step("rank reload", id) {
            player.services.game.players
                ?.reloadRank(player)
        }
        step("moderation data", id) { game.moderation.sendLoginData(player) }
        step("build pass rooms", id) { player.rooms.syncBuildPassRooms(id) }
        client.send(BadgePointLimits(game.achievements))
        sendSafetyLock(client, player)
        step("probation", id) { endServedProbation(id) }
        step("login achievements", id) { game.loginAchievements.progress(player, now()) }
        step("chest notices", id) { deliverChestNotices(client, id) }
        step("calendar", id) { openCalendar(client, player) }
        step("targeted offer", id) { sendTargetOffer(client, player) }
        markSessionStart(player)
        step("club", id) { ClubReactions.loggedIn(player) }
        step("rank items", id) { applyRankItems(player) }
        if (StarterSearches.handOut(player)) {
            game.threading.run(player.stats)
            client.send(NewNavigatorSavedSearches(player.profile.savedSearches))
        }
    }

    /** A failed statement is logged; the steps after it still run. */
    private inline fun step(
        what: String,
        playerId: Int,
        work: () -> Unit,
    ) = log.orFallback("Login step '$what' of player $playerId", Unit, work)

    /** Sent every login: the toolbar keeps its lock badge while the account is locked. */
    private fun sendSafetyLock(
        client: ClientConnection,
        player: Player,
    ) {
        val state = if (player.stats.standing.safetyLocked) SAFETY_LOCKED else SAFETY_UNLOCKED
        client.send(AccountSafetyLockStatusChangeOutCodec, AccountSafetyLockStatusChangeOut(state))
    }

    private fun endServedProbation(playerId: Int) {
        if (!config.getBoolean("moderation.sanction_ladder.is_on")) return
        val last = game.sanctions.latestOf(playerId) ?: return
        val served = last.probationEndsAt != 0 && now() >= last.probationEndsAt
        if (served) game.sanctions.endProbation(last.id)
    }

    /** The store keeps only the first five notices. */
    private fun deliverChestNotices(
        client: ClientConnection,
        playerId: Int,
    ) {
        val waiting =
            queries.select(PENDING_CHEST_NOTICES, playerId).rows { row ->
                val dialog =
                    ChestNotice.entries.getOrNull(row.getInt("reason"))?.let { reason ->
                        ChestNotices.dialog(
                            reason,
                            row.getString("actor_name"),
                            row.getInt("room_id"),
                            row.getString("chest_name"),
                        )
                    }
                row.getInt("id") to dialog
            }
        val lastRead = waiting.maxOfOrNull { it.first } ?: return
        waiting.mapNotNull { it.second }.forEach { client.send(NotificationDialogOutCodec, it) }
        queries.write(DELIVERED_CHEST_NOTICES, playerId, lastRead)
    }

    private fun openCalendar(
        client: ClientConnection,
        player: Player,
    ) {
        if (!config.getBoolean("calendar.is_open")) return
        val campaign = game.calendars.campaign(config.getValue("calendar.campaign")) ?: return
        if (game.calendars.show(player, campaign)) {
            client.send(InClientLinkOutCodec, InClientLinkOut("openView/calendar"))
        }
    }

    private fun sendTargetOffer(
        client: ClientConnection,
        player: Player,
    ) {
        val offer = game.catalog.runningTargetedOffer() ?: return
        val purchase = player.stats.offerPurchase(offer.id) ?: return
        client.send(TargetedOfferOutCodec, CatalogMessages.targetedOffer(offer, purchase))
    }

    /** Logout and saves never move the session start. */
    private fun markSessionStart(player: Player) {
        val profile = player.profile
        profile.lastOnlineAt = now()
        try {
            queries.write(SESSION_START_SQL, profile.lastOnlineAt, profile.id)
        } catch (e: SqlFailure) {
            log.error("Storing the login time of player {} failed", profile.id, e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(LoginFollowUps::class.java)
        private const val SAFETY_LOCKED = 0
        private const val SAFETY_UNLOCKED = 1
        private const val PENDING_CHEST_NOTICES =
            "SELECT n.id, n.room_id, n.reason, n.chest_name, COALESCE(p.name, '') AS actor_name " +
                "FROM wired_chest_notices n LEFT JOIN players p ON p.id = n.actor_id " +
                "WHERE n.player_id = ? ORDER BY n.id ASC LIMIT 5"
        private const val DELIVERED_CHEST_NOTICES = "DELETE FROM wired_chest_notices WHERE player_id = ? AND id <= ?"

        const val SESSION_START_SQL = "UPDATE players SET last_online_at = ? WHERE id = ?"
    }
}
