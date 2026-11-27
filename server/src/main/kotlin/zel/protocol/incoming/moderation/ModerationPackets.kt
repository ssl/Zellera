package zel.protocol.incoming.moderation

import zel.core.Texts
import zel.game.moderation.ModerationService
import zel.game.moderation.PlayerClassifier
import zel.game.moderation.ReportTopic
import zel.game.moderation.Sanctions
import zel.game.moderation.StaffActions
import zel.game.moderation.StaffMutes
import zel.game.moderation.StandingCounter
import zel.game.moderation.SupportTicket
import zel.game.moderation.TicketResolution
import zel.game.moderation.TradeLocks
import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.game.players.PlayerProfile
import zel.game.players.PlayerSettings
import zel.game.players.Players
import zel.game.players.connection
import zel.protocol.generated.CfhSanctionOutCodec
import zel.protocol.generated.CloseIssueDefaultActionIn
import zel.protocol.generated.CloseIssueDefaultActionInCodec
import zel.protocol.generated.CloseIssuesInCodec
import zel.protocol.generated.DefaultSanctionInCodec
import zel.protocol.generated.GetCfhChatlogInCodec
import zel.protocol.generated.GetModeratorPlayerInfoInCodec
import zel.protocol.generated.GetModeratorRoomInfoInCodec
import zel.protocol.generated.GetPlayerChatlogInCodec
import zel.protocol.generated.GetRoomChatlogInCodec
import zel.protocol.generated.GetRoomVisitsInCodec
import zel.protocol.generated.ModAlertInCodec
import zel.protocol.generated.ModBanInCodec
import zel.protocol.generated.ModKickInCodec
import zel.protocol.generated.ModMessageInCodec
import zel.protocol.generated.ModMuteInCodec
import zel.protocol.generated.ModToolPreferencesInCodec
import zel.protocol.generated.ModToolSanctionInCodec
import zel.protocol.generated.ModTradingLockInCodec
import zel.protocol.generated.ModerateRoomInCodec
import zel.protocol.generated.ModeratorActionInCodec
import zel.protocol.generated.ModeratorCautionOut
import zel.protocol.generated.ModeratorCautionOutCodec
import zel.protocol.generated.ModeratorPlayerInfoOutCodec
import zel.protocol.generated.ModeratorRoomInfoOut
import zel.protocol.generated.ModeratorRoomInfoOutCodec
import zel.protocol.generated.PeerPlayersClassificationInCodec
import zel.protocol.generated.PickIssuesIn
import zel.protocol.generated.PickIssuesInCodec
import zel.protocol.generated.PlayerChatlogOutCodec
import zel.protocol.generated.PlayerClassificationOut
import zel.protocol.generated.PlayerClassificationOutCodec
import zel.protocol.generated.ReleaseIssuesInCodec
import zel.protocol.generated.RoomChatlogOutCodec
import zel.protocol.generated.RoomPlayersClassificationInCodec
import zel.protocol.generated.RoomVisitsOutCodec
import zel.protocol.outgoing.generic.alerts.Broadcast
import zel.protocol.outgoing.moderation.ModerationChatlogs
import zel.protocol.outgoing.moderation.ModerationMessages
import zel.protocol.outgoing.moderation.ModerationReplies
import zel.protocol.outgoing.moderation.ModeratorPlayerInfo
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketIds
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.listing.RoomListings
import zel.room.port.RoomPort
import zel.util.SECONDS_PER_DAY
import zel.util.SECONDS_PER_HOUR

class ModerationPackets(
    private val manager: ModerationService,
    private val sanctions: Sanctions,
    private val players: Players,
    rooms: RoomListings,
    private val texts: Texts,
    userInfo: ModeratorPlayerInfo,
    issueChatlogs: IssueChatlogs,
    classifier: PlayerClassifier,
    private val clock: () -> Int,
    live: () -> RoomPort,
) : PacketPackage {
    override val name = "modtool"

    private val kickCooldown = ClientCooldown("modtool.kick", RATE_ACTION_MS)
    private val defaultSanctionCooldown = ClientCooldown("modtool.defaultsanction", RATE_ACTION_MS)
    private val windowCooldown = ClientCooldown("modtool.preferences", RATE_PREFERENCES_MS)
    private val cautionCooldown = ClientCooldown("modtool.caution", RATE_ACTION_MS)
    private val banCooldown = ClientCooldown("modtool.ban", RATE_ACTION_MS)
    private val muteCooldown = ClientCooldown("modtool.mute", RATE_ACTION_MS)
    private val tradeLockCooldown = ClientCooldown("modtool.tradelock", RATE_ACTION_MS)
    private val tradeLocks get() = manager.tradeLocks

    override val routes: List<PacketRoute<*>> =
        listOf(
            PacketRoute(PickIssuesInCodec) { s, m -> pickIssues(s, m) },
            PacketRoute(CloseIssueDefaultActionInCodec) { s, m -> closeWithDefaultSanction(s, m) },
            PacketRoute(ModKickInCodec) { s, m ->
                if (!kickCooldown.acquire(s.client)) return@PacketRoute
                withRight(s, Privilege.MODERATION_PLAYER_KICK) { moderator ->
                    val message = ModerationInput.cleaned(m.string1)
                    if (!PacketIds.isId(m.int1) ||
                        !ModerationInput.isMessage(message)
                    ) {
                        return@withRight
                    }
                    val kicked = players.find(m.int1)?.let { manager.actions.kick(moderator, it, message) } == true
                    ModerationReplies.actionResult(s.client, m.int1, kicked)
                }
            },
            PacketRoute(ModMessageInCodec) { s, m ->
                withRight(s, Privilege.MODERATION_PLAYER_ALERT) { moderator ->
                    val message = ModerationInput.cleaned(m.string1)
                    if (!PacketIds.isId(m.int1) ||
                        !ModerationInput.isMessage(message)
                    ) {
                        return@withRight
                    }
                    val addressed = players.find(m.int1) ?: return@withRight
                    // an answer to a report is owed to its reporter whatever their rank
                    if (manager.tickets.isAnswering(moderator.profile.id, addressed.profile.id)) {
                        ModerationReplies.issueResponseAlert(addressed.connection, message)
                    } else {
                        manager.actions.message(moderator, addressed, message)
                    }
                }
            },
            PacketRoute(ModerateRoomInCodec) { s, m ->
                withRight(s, Privilege.MODERATION_ROOM_INTERVENE) { moderator ->
                    if (!PacketIds.isId(m.roomId)) return@withRight
                    val room = rooms.room(m.roomId) ?: return@withRight
                    val measures = StaffActions.RoomMeasures(m.kickUsers == 1, m.lockDoor == 1, m.changeTitle == 1)
                    manager.actions.intervene(room, moderator, measures)
                }
            },
            PacketRoute(CloseIssuesInCodec) { s, m ->
                withRight(s, Privilege.MODERATION_TICKETS) { moderator ->
                    if (m.state !in 1..3 ||
                        !ModerationInput.isTicketBatch(m.issueIds.size)
                    ) {
                        return@withRight
                    }
                    val resolution =
                        when (m.state) {
                            ModerationReplies.USELESS -> TicketResolution.USELESS
                            ModerationReplies.ABUSIVE -> TicketResolution.ABUSIVE
                            else -> TicketResolution.HANDLED
                        }
                    for (id in m.issueIds) {
                        val issue = ownedTicket(id, moderator) ?: continue
                        if (manager.tickets.close(issue, resolution)) {
                            manager.tickets.tellReporter(players.find(issue.reporterId), resolution)
                        }
                    }
                }
            },
            PacketRoute(DefaultSanctionInCodec) { s, m ->
                if (!defaultSanctionCooldown.acquire(s.client)) return@PacketRoute
                withRight(s, ModToolRights.SANCTION_WINDOW) { moderator ->
                    val message = ModerationInput.cleaned(m.string1)
                    if (!PacketIds.isId(m.int1) ||
                        !PacketIds.isId(m.int2)
                    ) {
                        return@withRight
                    }
                    val topic = manager.catalog.topic(m.int2)
                    val given = topic != null && giveDefault(moderator, m.int1, topic, message)
                    ModerationReplies.actionResult(s.client, m.int1, given)
                }
            },
            PacketRoute(ModToolSanctionInCodec) { s, m ->
                withRight(s, Privilege.MODERATION_TOOL) { moderator ->
                    if (!PacketIds.isId(m.int3) || manager.catalog.topic(m.int3) == null) return@withRight
                    var targetId = m.int2
                    if (PacketIds.isId(m.int1)) {
                        if (!moderator.hasPermission(Privilege.MODERATION_TICKETS)) return@withRight
                        val issue = ownedTicket(m.int1, moderator) ?: return@withRight
                        issue.category = m.int3
                        manager.tickets.save(issue)
                        manager.tickets.announce(issue)
                        targetId = issue.accusedId
                    }
                    val shown = sanctions.defaultFor(m.int3, targetId).proposal()
                    s.send(CfhSanctionOutCodec, shown.message(m.int1, m.int2))
                }
            },
            PacketRoute(ModToolPreferencesInCodec) { s, m ->
                if (!windowCooldown.acquire(s.client)) return@PacketRoute
                withRight(s, Privilege.MODERATION_TOOL) { moderator ->
                    val sane =
                        ModeratorWindowBounds.isSaneCoordinate(m.x) && ModeratorWindowBounds.isSaneCoordinate(m.y) &&
                            ModeratorWindowBounds.isSaneSize(m.width) && ModeratorWindowBounds.isSaneSize(m.height)
                    if (sane) {
                        moderator.stats.settings.update(
                            PlayerSettings.ModeratorWindow(m.x, m.y, m.width, m.height),
                        )
                    }
                }
            },
            PacketRoute(ReleaseIssuesInCodec) { s, m ->
                withRight(s, Privilege.MODERATION_TICKETS) { moderator ->
                    if (!ModerationInput.isTicketBatch(m.issueIds.size)) return@withRight
                    for (id in m.issueIds) {
                        if (!PacketIds.isId(id)) continue
                        manager.tickets.byId(id)?.let { manager.tickets.release(it, moderator) }
                    }
                }
            },
            PacketRoute(GetCfhChatlogInCodec) { s, m ->
                withRight(s, Privilege.MODERATION_TICKETS) { _ -> issueChatlogs.send(s, m.int1) }
            },
            PacketRoute(GetRoomChatlogInCodec) { s, m ->
                withRight(s, Privilege.MODERATION_ROOM_CHATLOG) { _ ->
                    if (!PacketIds.isId(m.int2)) return@withRight
                    val room = rooms.room(m.int2) ?: return@withRight
                    val lines = manager.chats.roomLines(room.id)
                    s.send(
                        RoomChatlogOutCodec,
                        ModerationChatlogs.roomLog(room.name, room.id, lines),
                    )
                }
            },
            PacketRoute(GetModeratorRoomInfoInCodec) { s, m ->
                withRight(s, Privilege.MODERATION_ROOM_INFO) { _ ->
                    if (!PacketIds.isId(m.int1)) return@withRight
                    val room = rooms.room(m.int1) ?: return@withRight
                    ModerationReplies.send(
                        s.client,
                        ModeratorRoomInfoOutCodec,
                        ModeratorRoomInfoOut(
                            int1 = room.id,
                            int2 = room.userCount,
                            boolean1 = room.ownerId in live().playersIn(room.id),
                            int3 = room.ownerId,
                            string1 = room.ownerName,
                            boolean2 = true,
                            string2 = room.name,
                            string3 = room.description,
                            list1 = room.tagList,
                        ),
                    )
                }
            },
            PacketRoute(GetRoomVisitsInCodec) { s, m ->
                withRight(s, Privilege.MODERATION_PLAYER_CHATLOG) { moderator ->
                    val profile = reachedProfile(moderator, m.int1) ?: return@withRight
                    s.send(
                        RoomVisitsOutCodec,
                        ModerationMessages.roomVisits(
                            profile.id,
                            profile.name,
                            manager.visits.visitsOf(m.int1),
                        ),
                    )
                }
            },
            PacketRoute(GetPlayerChatlogInCodec) { s, m ->
                withRight(s, Privilege.MODERATION_PLAYER_CHATLOG) { moderator ->
                    val profile = reachedProfile(moderator, m.int1) ?: return@withRight
                    s.send(
                        PlayerChatlogOutCodec,
                        ModerationChatlogs.playerLog(
                            m.int1,
                            profile.name,
                            manager.visits.visitsWithChat(m.int1),
                        ),
                    )
                }
            },
            PacketRoute(GetModeratorPlayerInfoInCodec) { s, m ->
                withRight(s, Privilege.MODERATION_TOOL) { moderator ->
                    if (m.int1 <= 0) return@withRight
                    userInfo.read(m.int1, sheetView(moderator, m.int1))?.let {
                        ModerationReplies.send(s.client, ModeratorPlayerInfoOutCodec, it)
                    }
                }
            },
            PacketRoute(ModeratorActionInCodec) { s, m ->
                withRight(s, Privilege.MODERATION_ROOM_ALERT) { moderator ->
                    val message = ModerationInput.cleaned(m.string1)
                    if (!ModerationInput.isMessage(message)) return@withRight
                    val port = live()
                    port.roomOf(moderator.profile.id)?.let {
                        Broadcast(message).compose()?.let { alert -> port.send(it, alert) }
                    }
                }
            },
            PacketRoute(ModAlertInCodec) { s, m ->
                if (!cautionCooldown.acquire(s.client)) return@PacketRoute
                val message = ModerationInput.cleaned(m.string1)
                if (!validSanction(m.int1, m.int2, message)) return@PacketRoute
                withRight(s, Privilege.MODERATION_PLAYER_ALERT) { moderator ->
                    val target = players.find(m.int1)
                    if (target == null) {
                        notFound(s, m.int1)
                        return@withRight
                    }
                    val cautioned = manager.actions.caution(moderator, target, message)
                    if (cautioned) manager.standing.count(m.int1, StandingCounter.WARNINGS_RECEIVED)
                    ModerationReplies.actionResult(s.client, m.int1, cautioned)
                }
            },
            PacketRoute(ModBanInCodec) { s, m ->
                if (!banCooldown.acquire(s.client)) return@PacketRoute
                val message = ModerationInput.cleaned(m.string1)
                if (!validSanction(m.int1, m.int2, message)) return@PacketRoute
                withRight(s, Privilege.MODERATION_PLAYER_BAN) { moderator ->
                    val issued =
                        manager.banning
                            .ban(
                                m.int1,
                                moderator,
                                message,
                                banDuration(m.int3),
                                m.int2,
                                m.int3 == BAN_AVATAR_ONLY_100_YEARS,
                            )
                    ModerationReplies.actionResult(s.client, m.int1, issued.isNotEmpty())
                }
            },
            PacketRoute(ModMuteInCodec) { s, m ->
                if (!muteCooldown.acquire(s.client)) return@PacketRoute
                val message = ModerationInput.cleaned(m.string1)
                if (!validSanction(m.int1, m.int2, message)) return@PacketRoute
                withRight(s, Privilege.MODERATION_PLAYER_MUTE) { moderator -> mute(s, moderator, m.int1, message) }
            },
            PacketRoute(PeerPlayersClassificationInCodec) { s, m ->
                withRight(s, Privilege.MODERATION_ROOM_INFO) { _ ->
                    val name = ModerationInput.cleaned(m.string1)
                    if (name.isNotEmpty()) sendClassification(s, classifier.peersOf(name))
                }
            },
            PacketRoute(RoomPlayersClassificationInCodec) { s, m ->
                withRight(s, Privilege.MODERATION_ROOM_INFO) { _ ->
                    m.string1.trim().toIntOrNull()?.takeIf(PacketIds::isId)?.let {
                        sendClassification(s, classifier.inRoom(it))
                    }
                }
            },
            PacketRoute(ModTradingLockInCodec) { s, m ->
                if (!tradeLockCooldown.acquire(s.client)) return@PacketRoute
                val message = ModerationInput.cleaned(m.string1)
                if (!validSanction(m.int1, m.int3, message) || m.int2 <= 0) return@PacketRoute
                withRight(s, Privilege.MODERATION_PLAYER_TRADE_LOCK) { moderator ->
                    tradeLock(s, moderator, m.int1, message, m.int2)
                }
            },
        )

    private fun sendClassification(
        s: PacketSession,
        classified: List<PlayerClassifier.Classified>,
    ) {
        val listed = classified.map { PlayerClassificationOut.List1Item(it.id, it.name, it.kind) }
        s.send(PlayerClassificationOutCodec, PlayerClassificationOut(listed))
    }

    private fun pickIssues(
        session: PacketSession,
        m: PickIssuesIn,
    ) {
        val player = moderatorWith(session, Privilege.MODERATION_TICKETS) ?: return
        if (!ModerationInput.isTicketBatch(m.issueIds.size)) return
        val failed = ArrayList<SupportTicket>()
        var picked = false
        for (ticketId in m.issueIds.filter(PacketIds::isId)) {
            val issue = manager.tickets.byId(ticketId) ?: continue
            if (!manager.tickets.pick(issue, player)) {
                failed.add(issue)
                continue
            }
            picked = true
            val sanction = sanctions.defaultFor(issue.category, issue.accusedId).proposal()
            session.send(CfhSanctionOutCodec, sanction.message(issue.id, -1))
        }
        if (failed.isNotEmpty()) {
            failed.forEach { ModerationReplies.issueInfo(session.client, it, clock()) }
            ModerationReplies.issuePickFailed(session.client, failed, m.retryEnabled, m.retryCount + 1)
        } else if (!picked) {
            player.alert(texts.text("moderation.ticket.pick_failed"))
        }
    }

    private fun closeWithDefaultSanction(
        session: PacketSession,
        m: CloseIssueDefaultActionIn,
    ) {
        val player = moderatorWith(session, Privilege.MODERATION_TICKETS) ?: return
        if (!PacketIds.isId(m.issueId) || !PacketIds.isId(m.topic)) return
        val issue = manager.tickets.byId(m.issueId) ?: return
        if (!ModerationInput.ownsTicket(issue, player)) {
            player.alert(texts.text("moderation.ticket.not_yours"))
            return
        }
        val topic = manager.catalog.topic(m.topic) ?: return
        if (giveDefault(player, issue.accusedId, topic, "")) manager.tickets.close(issue, TicketResolution.HANDLED)
    }

    /** The default sanction as the tool showed it; a mute or a ban needs that right, an alert only the tool. */
    private fun giveDefault(
        moderator: Player,
        accusedId: Int,
        topic: ReportTopic,
        message: String,
    ): Boolean {
        if (!ModToolRights.mayGive(moderator, sanctions.defaultFor(topic.id, accusedId).kind)) return false
        val reason = message.takeIf(ModerationInput::isMessage) ?: topic.defaultSanction?.message.orEmpty()
        return sanctions.giveDefault(accusedId, moderator, topic.id, reason)
    }

    /** The mod tool's mute button: the one length it offers. */
    private fun mute(
        s: PacketSession,
        moderator: Player,
        userId: Int,
        message: String,
    ) {
        val profile = players.profileOf(userId)
        if (profile == null) {
            notFound(s, userId)
            return
        }
        if (!manager.reach.reaches(moderator, profile)) return
        val done = manager.mutes.give(userId, StaffMutes.TOOL_SECONDS) > 0
        if (done) {
            players.find(userId)?.client?.send(ModeratorCautionOutCodec, ModeratorCautionOut(message, ""))
            moderator.whisper(texts.text("moderation.mute.done").replace("%player%", profile.name))
            manager.standing.count(userId, StandingCounter.WARNINGS_RECEIVED)
        }
        ModerationReplies.actionResult(s.client, userId, done)
    }

    private fun tradeLock(
        s: PacketSession,
        moderator: Player,
        userId: Int,
        message: String,
        minutes: Int,
    ) {
        val profile = players.profileOf(userId)
        if (profile == null) {
            notFound(s, userId)
            return
        }
        if (!manager.reach.reaches(moderator, profile)) return
        val granted = tradeLocks.grantedMinutes(moderator, minutes)
        if (granted == null) {
            ModerationReplies.actionResult(s.client, userId, false)
            return
        }
        val until = TradeLocks.until(clock(), granted)
        val online = players.find(userId)
        val done =
            if (online != null) {
                tradeLocks.lock(online, until)
                online.client?.send(ModeratorCautionOutCodec, ModeratorCautionOut(message, ""))
                true
            } else {
                tradeLocks.lockOffline(userId, until)
            }
        ModerationReplies.actionResult(s.client, userId, done)
    }

    /** The profile of a player whose record [moderator] may read; null for anyone they do not reach. */
    private fun reachedProfile(
        moderator: Player,
        playerId: Int,
    ): PlayerProfile? {
        if (!PacketIds.isId(playerId)) return null
        return players.profileOf(playerId)?.takeIf { manager.reach.reaches(moderator, it) }
    }

    private fun sheetView(
        moderator: Player,
        playerId: Int,
    ): ModeratorPlayerInfo.View =
        when {
            !manager.reach.canModerate(moderator, playerId) -> ModeratorPlayerInfo.View.CARD
            moderator.hasPermission(Privilege.MODERATION_PLAYER_BAN) -> ModeratorPlayerInfo.View.RECORD_WITH_IDENTITY
            else -> ModeratorPlayerInfo.View.RECORD
        }

    private fun notFound(
        s: PacketSession,
        userId: Int,
    ) {
        ModerationReplies.actionResult(s.client, userId, false)
        ModerationReplies.issueHandled(
            s.client,
            texts.text("moderation.player.unknown"),
        )
    }

    private fun validSanction(
        userId: Int,
        topic: Int,
        message: String,
    ) = PacketIds.isId(userId) &&
        PacketIds.isId(topic) &&
        ModerationInput.isMessage(message)

    private fun banDuration(banType: Int): Int =
        when (banType) {
            BAN_18_HOURS -> BAN_18_HOURS_SECONDS
            BAN_7_DAYS -> BAN_7_DAYS_SECONDS
            BAN_30_DAYS_STEP_1, BAN_30_DAYS_STEP_2 -> BAN_30_DAYS_SECONDS
            BAN_100_YEARS, BAN_AVATAR_ONLY_100_YEARS -> BAN_PERMANENT_SECONDS
            else -> 0
        }

    private fun ownedTicket(
        id: Int,
        moderator: Player,
    ): SupportTicket? {
        if (!PacketIds.isId(id)) return null
        return manager.tickets.byId(id)?.takeIf { ModerationInput.ownsTicket(it, moderator) }
    }

    private inline fun withRight(
        s: PacketSession,
        vararg anyOf: Privilege,
        block: (Player) -> Unit,
    ) {
        moderatorWith(s, anyOf.asList())?.let(block)
    }

    private inline fun withRight(
        s: PacketSession,
        anyOf: List<Privilege>,
        block: (Player) -> Unit,
    ) {
        moderatorWith(s, anyOf)?.let(block)
    }

    private fun moderatorWith(
        s: PacketSession,
        vararg anyOf: Privilege,
    ): Player? = moderatorWith(s, anyOf.asList())

    private fun moderatorWith(
        s: PacketSession,
        anyOf: List<Privilege>,
    ): Player? {
        val player = s.client.player ?: return null
        if (ModToolRights.allows(player, anyOf)) return player
        s.client.warnings.refused(NO_RIGHT)
        return null
    }

    private companion object {
        const val NO_RIGHT = "no right"
        const val RATE_ACTION_MS = 2000L
        const val RATE_PREFERENCES_MS = 500L
        const val BAN_18_HOURS_SECONDS = 18 * SECONDS_PER_HOUR
        const val BAN_7_DAYS_SECONDS = 7 * SECONDS_PER_DAY
        const val BAN_30_DAYS_SECONDS = 30 * SECONDS_PER_DAY
        const val BAN_18_HOURS = 3
        const val BAN_7_DAYS = 4
        const val BAN_30_DAYS_STEP_1 = 5
        const val BAN_30_DAYS_STEP_2 = 7
        const val BAN_100_YEARS = 6
        const val BAN_AVATAR_ONLY_100_YEARS = 106

        /** PlayerBanning caps the expiry at the last second an Int holds, so this is the longest ban there is. */
        const val BAN_PERMANENT_SECONDS = Int.MAX_VALUE
    }
}
