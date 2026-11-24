package zel.protocol.incoming.moderation

import zel.core.Configuration
import zel.core.rules.rules
import zel.game.guilds.forums.ForumThreadStore
import zel.game.items.ItemDataUrls
import zel.game.items.ItemInteraction
import zel.game.moderation.ChatlogLine
import zel.game.moderation.ModerationService
import zel.game.moderation.ReportTopic
import zel.game.moderation.SupportTicket
import zel.game.moderation.TicketResolution
import zel.game.moderation.TicketState
import zel.game.moderation.TicketType
import zel.game.players.Player
import zel.game.players.PlayerProfile
import zel.game.players.Players
import zel.net.clients.ClientConnection
import zel.protocol.generated.CallForHelpFromForumMessageInCodec
import zel.protocol.generated.CallForHelpFromForumThreadInCodec
import zel.protocol.generated.CallForHelpFromIMIn
import zel.protocol.generated.CallForHelpFromIMInCodec
import zel.protocol.generated.CallForHelpFromPhotoInCodec
import zel.protocol.generated.CallForHelpIn
import zel.protocol.generated.CallForHelpInCodec
import zel.protocol.generated.CallForHelpPendingCallsDeletedOut
import zel.protocol.generated.CallForHelpPendingCallsDeletedOutCodec
import zel.protocol.generated.CallForHelpPendingCallsOutCodec
import zel.protocol.generated.ChatReviewSessionCreateInCodec
import zel.protocol.generated.DeletePendingCallsForHelpInCodec
import zel.protocol.generated.GetGuideReportingStatusInCodec
import zel.protocol.generated.GetPendingCallsForHelpInCodec
import zel.protocol.generated.GuideReportingStatusOutCodec
import zel.protocol.outgoing.generic.alerts.BubbleAlertMessage
import zel.protocol.outgoing.moderation.ModerationMessages
import zel.protocol.outgoing.moderation.ModerationReplies
import zel.protocol.outgoing.rooms.users.IgnoreResult
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketIds
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.protocol.typed.player

class ReportPackets(
    private val manager: ModerationService,
    private val players: Players,
    private val config: Configuration,
    private val forumThreads: ForumThreadStore,
    private val clock: () -> Int,
) : PacketPackage {
    override val name = "report"

    private val deleteCooldown = ClientCooldown("modtool.deletepending", RATE_DELETE_MS)
    private val reportCooldown = ClientCooldown("report", REPORT_GAP_MILLIS)

    override val routes: List<PacketRoute<*>> =
        listOf(
            PacketRoute(CallForHelpInCodec) { s, m -> if (mayReport(s)) callForHelp(s, m) },
            PacketRoute(CallForHelpFromIMInCodec) { s, m -> if (mayReport(s)) callForHelpFromIm(s, m) },
            PacketRoute(
                ChatReviewSessionCreateInCodec,
            ) { s, m -> if (mayReport(s)) bullyReport(s, m.userId, m.roomId) },
            PacketRoute(CallForHelpFromForumMessageInCodec) { s, m ->
                if (!mayReport(s)) return@PacketRoute
                val message = ModerationInput.cleaned(m.string1)
                if (listOf(m.int1, m.int2, m.int3, m.int4).any { !PacketIds.isId(it) } ||
                    !ModerationInput.isMessage(message)
                ) {
                    return@PacketRoute
                }
                forumReport(
                    s,
                    m.int1,
                    m.int2,
                    m.int4,
                    m.int3,
                    ModerationInput.withReporterContact(message, m.string2, m.string3),
                )
            },
            PacketRoute(CallForHelpFromForumThreadInCodec) { s, m ->
                if (!mayReport(s)) return@PacketRoute
                val message = ModerationInput.cleaned(m.string1)
                if (listOf(m.int1, m.int2, m.int3).any { !PacketIds.isId(it) } ||
                    !ModerationInput.isMessage(message)
                ) {
                    return@PacketRoute
                }
                forumReport(
                    s,
                    m.int1,
                    m.int2,
                    m.int3,
                    -1,
                    ModerationInput.withReporterContact(message, m.string2, m.string3),
                )
            },
            PacketRoute(CallForHelpFromPhotoInCodec) { s, m ->
                if (mayReport(s)) photoReport(s, m.roomId, m.topicId, m.itemId, m.reporterName, m.reporterEmail)
            },
            PacketRoute(GetPendingCallsForHelpInCodec) { s, _ ->
                val player = s.client.player ?: return@PacketRoute
                s.send(
                    CallForHelpPendingCallsOutCodec,
                    ModerationMessages.pendingCalls(manager.tickets.openOf(player)),
                )
            },
            PacketRoute(GetGuideReportingStatusInCodec) { s, _ -> reportingStatus(s) },
            PacketRoute(DeletePendingCallsForHelpInCodec) { s, _ ->
                val player = s.client.player ?: return@PacketRoute
                if (!deleteCooldown.acquire(s.client)) return@PacketRoute
                manager.tickets.openOf(player).forEach { manager.tickets.close(it, TicketResolution.WITHDRAWN) }
                s.client.send(CallForHelpPendingCallsDeletedOutCodec, CallForHelpPendingCallsDeletedOut)
            },
        )

    private fun bullyReport(
        s: PacketSession,
        userId: Int,
        roomId: Int,
    ) {
        val reporter = s.client.player ?: return
        val validIds =
            PacketIds.isId(userId) &&
                PacketIds.isId(roomId) &&
                userId != reporter.profile.id
        val reported = if (validIds && reporter.rooms.roomOf(userId) == roomId) players.find(userId) else null
        when {
            !reporter.stats.standing.mayTalk() -> ModerationReplies.helperRequestDisabled(s.client)
            reported != null -> ModerationReplies.bullyReported(s.client, fileBullyReport(reporter, reported, roomId))
        }
    }

    private fun fileBullyReport(
        reporter: Player,
        reported: Player,
        roomId: Int,
    ): Int =
        if (manager.tickets.filedBy(reporter.profile.id).any { it.accusedId == reported.profile.id }) {
            BULLY_ALREADY_REPORTED
        } else {
            val chatLog = manager.chats.roomLines(roomId)
            if (chatLog.isEmpty()) {
                BULLY_NO_CHAT
            } else {
                val issue =
                    manager.tickets.draft(
                        TicketType.NORMAL,
                        reporter.profile.id,
                        reporter.profile.name,
                        reported.profile.id,
                        reported.profile.name,
                        roomId,
                        "",
                    )
                issue.pickedLines = chatLog
                file(issue)
                BULLY_RECEIVED
            }
        }

    private fun forumReport(
        s: PacketSession,
        groupId: Int,
        threadId: Int,
        topicId: Int,
        commentId: Int,
        message: String,
    ) {
        val reporter = s.client.player
        val thread = forumThreads.thread(threadId)?.takeIf { it.guildId == groupId }
        if (reporter == null || thread == null || manager.catalog.topic(topicId) == null) return
        val accusedId =
            if (commentId == -1) {
                thread.openerId
            } else {
                forumThreads.postIn(thread, commentId)?.authorId ?: return
            }
        val accused = players.profileOf(accusedId)
        val issue =
            manager.tickets.draft(
                TicketType.FORUM,
                reporter.profile.id,
                reporter.profile.name,
                accusedId,
                accused?.name.orEmpty(),
                0,
                message,
            )
        issue.category = topicId
        issue.guildId = groupId
        issue.forumThreadId = threadId
        if (commentId != -1) issue.forumPostId = commentId
        ModerationReplies.reportReceived(s.client, ModerationReplies.REPORT_RECEIVED, message)
        file(issue)
    }

    private fun photoReport(
        s: PacketSession,
        roomId: Int,
        topicId: Int,
        itemId: Int,
        reporterName: String,
        reporterEmail: String,
    ) {
        val reporter = s.client.player
        val valid = listOf(roomId, topicId, itemId).all { PacketIds.isId(it) }
        val item =
            if (valid &&
                manager.catalog.topic(topicId) != null
            ) {
                reporter
                    ?.rooms
                    ?.furniIn(roomId, itemId)
                    ?.let {
                        reporter.services.game.items
                            ?.loadFurni(it.id)
                    }
            } else {
                null
            }
        val owner =
            item
                ?.takeIf {
                    it.definition.interaction.name == ItemInteraction.EXTERNAL_IMAGE
                }?.let { players.profileOf(it.userId) }
        if (reporter == null || owner == null) return
        val issue =
            manager.tickets.draft(
                TicketType.PHOTO,
                reporter.profile.id,
                reporter.profile.name,
                owner.id,
                owner.name,
                roomId,
                ModerationInput.withReporterContact("", reporterName, reporterEmail),
            )
        issue.category = topicId
        issue.photo = item
        ModerationReplies.reportReceived(s.client, ModerationReplies.REPORT_RECEIVED, "")
        file(issue)
    }

    private fun reportingStatus(s: PacketSession) {
        val player = s.client.player ?: return
        val latest = manager.tickets.filedBy(player.profile.id).maxOfOrNull { it.createdAt }
        val code =
            when {
                latest != null && clock() - latest < config.getInt("moderation.reports.gap.seconds") -> TOO_RECENT
                else -> START_REPORT
            }
        val caseType = if (code == START_REPORT) 0 else 1
        s.send(GuideReportingStatusOutCodec, ModerationMessages.guideReportingStatus(code, caseType))
    }

    private fun callForHelp(
        session: PacketSession,
        m: CallForHelpIn,
    ) {
        val player = session.client.player ?: return
        if (!player.stats.standing.mayTalk()) {
            ModerationReplies.helperRequestDisabled(session.client)
            return
        }
        val text = ModerationInput.cleaned(m.message)
        val lines = pickedLines(m.lines.map { it.userId to it.text }) ?: return
        val valid =
            ModerationInput.isRoomChatCount(m.lines.size) &&
                ModerationInput.isMessage(text) &&
                m.topic > 0 &&
                (m.userId == -1 || PacketIds.isId(m.userId)) &&
                PacketIds.isId(m.roomId)
        if (!valid) return
        val message = ModerationInput.withReporterContact(text, m.reporterName, m.reporterEmail)
        val topic = manager.catalog.topic(m.topic) ?: return
        if (m.userId == -1) {
            roomReport(session, player, m.roomId, message, topic)
        } else {
            userReport(session, player, m, message, topic, lines)
        }
    }

    private fun roomReport(
        session: PacketSession,
        player: Player,
        roomId: Int,
        message: String,
        topic: ReportTopic,
    ) {
        val room = player.rooms.summary(roomId)
        val issue =
            manager.tickets.draft(
                TicketType.ROOM,
                player.profile.id,
                player.profile.name,
                room?.ownerId ?: 0,
                room?.ownerName.orEmpty(),
                roomId,
                message,
            )
        issue.category = topic.id
        if (topic.urgent) issue.priority = SupportTicket.URGENT
        session.client.send(created(topic))
        file(issue)
        if (topic.closesItself) closeAutomatically(session.client, player, issue, topic)
    }

    private fun userReport(
        session: PacketSession,
        player: Player,
        m: CallForHelpIn,
        message: String,
        topic: ReportTopic,
        lines: List<Pair<Int, String>>,
    ) {
        val reported = players.find(m.userId)
        // a user who went offline after the reporter picked his lines is still reported
        val profile = reported?.profile ?: players.readProfile(m.userId) ?: return
        val issue =
            manager.tickets.draft(
                TicketType.NORMAL,
                player.profile.id,
                player.profile.name,
                profile.id,
                profile.name,
                m.roomId,
                message,
            )
        issue.category = m.topic
        if (topic.urgent) issue.priority = SupportTicket.URGENT
        issue.pickedLines = keptLines(lines, player, profile)
        file(issue)
        session.client.send(created(topic))
        if (topic.closesItself) closeAutomatically(session.client, player, issue, topic)
    }

    /** Null for a bad line. */
    private fun pickedLines(lines: List<Pair<Int, String>>): List<Pair<Int, String>>? =
        lines
            .map { (author, text) -> author to ModerationInput.cleaned(text) }
            .takeIf { picked ->
                picked.all { (author, text) ->
                    PacketIds.isId(author) && ModerationInput.isPrivateChatLine(text)
                }
            }

    private fun keptLines(
        lines: List<Pair<Int, String>>,
        reporter: Player,
        reported: PlayerProfile,
    ): ArrayList<ChatlogLine> {
        val reporterId = reporter.profile.id
        val kept = ArrayList<ChatlogLine>(lines.size)
        for ((author, text) in lines) {
            if (!ModerationInput.isPrivateChatAuthor(author, reporterId, reported.id)) continue
            val name = if (author == reported.id) reported.name else reporter.profile.name
            kept.add(ChatlogLine(0, author, name, text))
        }
        return kept
    }

    private fun callForHelpFromIm(
        session: PacketSession,
        m: CallForHelpFromIMIn,
    ) {
        val player = session.client.player ?: return
        if (!player.stats.standing.mayTalk()) {
            ModerationReplies.helperRequestDisabled(session.client)
            return
        }
        val text = ModerationInput.cleaned(m.message)
        val valid =
            ModerationInput.isMessage(text) &&
                m.topic > 0 &&
                PacketIds.isId(m.userId) &&
                m.userId != player.profile.id &&
                ModerationInput.isPrivateChatCount(m.lines.size)
        if (!valid) return
        val target = players.find(m.userId)?.profile ?: players.readProfile(m.userId) ?: return
        val reporterId = player.profile.id
        val logs = ArrayList<ChatlogLine>(m.lines.size)
        for (line in m.lines) {
            val said = ModerationInput.cleaned(line.text)
            if (!ModerationInput.isPrivateChatAuthor(line.userId, reporterId, target.id) ||
                !ModerationInput.isPrivateChatLine(said)
            ) {
                return
            }
            val name = if (line.userId == target.id) target.name else player.profile.name
            logs.add(ChatlogLine(0, line.userId, name, said))
        }
        val issue =
            manager.tickets.draft(
                TicketType.MESSENGER,
                reporterId,
                player.profile.name,
                m.userId,
                target.name,
                0,
                ModerationInput.withReporterContact(text, m.reporterName, m.reporterEmail),
            )
        issue.category = m.topic
        issue.pickedLines = logs
        file(issue)
    }

    /** None past the waiting limit, which sends the waiting list instead. */
    private fun mayReport(s: PacketSession): Boolean {
        val player = s.client.player ?: return false
        if (!reportCooldown.acquire(s.client)) return false
        val waiting = manager.tickets.filedBy(player.profile.id)
        if (waiting.size < rules.limits.reportsOpenPerPlayer) return true
        s.send(CallForHelpPendingCallsOutCodec, ModerationMessages.pendingCalls(waiting))
        return false
    }

    private fun file(issue: SupportTicket) {
        manager.tickets.file(issue)
    }

    private fun created(topic: ReportTopic): BubbleAlertMessage {
        val site = ItemDataUrls.site(config)
        val keys = LinkedHashMap<String, String>()
        if (!topic.replyWithoutLink) keys["linkUrl"] = if (site.isEmpty()) "" else "$site/safety"
        keys["message"] = topic.reply
        return BubbleAlertMessage(CFH_CREATED, keys)
    }

    private fun closeAutomatically(
        client: ClientConnection,
        player: Player,
        issue: SupportTicket,
        topic: ReportTopic,
    ) {
        player.services.threading.run(
            {
                if (issue.state == TicketState.OPEN) {
                    if (topic.ignoresAccused) {
                        player.stats.contacts.ignore(issue.accusedId)
                        players.find(issue.accusedId)?.let {
                            client.send(IgnoreResult(it.profile.id))
                        }
                    }
                    manager.tickets.close(issue, TicketResolution.AUTOMATIC)
                }
            },
            AUTO_CLOSE_MILLIS,
        )
    }

    private companion object {
        const val CFH_CREATED = "cfh.created"
        const val AUTO_CLOSE_MILLIS = 1_000L
        const val RATE_DELETE_MS = 1000L
        const val BULLY_RECEIVED = 0
        const val BULLY_NO_CHAT = 2
        const val BULLY_ALREADY_REPORTED = 3
        const val START_REPORT = 0
        const val TOO_RECENT = 3
    }
}

internal const val REPORT_GAP_MILLIS = 4_000L
