package zel.protocol.outgoing.moderation

import zel.game.moderation.SupportTicket
import zel.net.clients.ClientConnection
import zel.protocol.generated.CallForHelpDisabledNotifyOut
import zel.protocol.generated.CallForHelpDisabledNotifyOutCodec
import zel.protocol.generated.CallForHelpReplyOut
import zel.protocol.generated.CallForHelpReplyOutCodec
import zel.protocol.generated.CallForHelpResultOut
import zel.protocol.generated.CallForHelpResultOutCodec
import zel.protocol.generated.GuideTicketCreationResultOut
import zel.protocol.generated.GuideTicketCreationResultOutCodec
import zel.protocol.generated.IssueCloseNotificationOut
import zel.protocol.generated.IssueCloseNotificationOutCodec
import zel.protocol.generated.IssueInfoOutCodec
import zel.protocol.generated.IssuePickFailedOutCodec
import zel.protocol.generated.ModeratorActionResultOut
import zel.protocol.generated.ModeratorActionResultOutCodec
import zel.protocol.generated.ModeratorToolPreferencesOut
import zel.protocol.generated.ModeratorToolPreferencesOutCodec
import zel.protocol.generated.OutgoingCodec

object ModerationReplies {
    const val HANDLED = 0
    const val USELESS = 1
    const val ABUSIVE = 2
    const val REPORT_RECEIVED = 0

    fun <T> send(
        client: ClientConnection,
        codec: OutgoingCodec<T>,
        message: T,
    ) = client.send(codec, message)

    fun actionResult(
        client: ClientConnection,
        userId: Int,
        success: Boolean,
    ) = send(client, ModeratorActionResultOutCodec, ModeratorActionResultOut(userId, success))

    fun issueHandled(
        client: ClientConnection,
        code: Int,
    ) = send(client, IssueCloseNotificationOutCodec, IssueCloseNotificationOut(code, ""))

    fun issueHandled(
        client: ClientConnection,
        message: String,
    ) = send(client, IssueCloseNotificationOutCodec, IssueCloseNotificationOut(0, message))

    fun issueResponseAlert(
        client: ClientConnection,
        message: String,
    ) = send(client, CallForHelpReplyOutCodec, CallForHelpReplyOut(message))

    fun reportReceived(
        client: ClientConnection,
        code: Int,
        message: String,
    ) = send(client, CallForHelpResultOutCodec, CallForHelpResultOut(code, message))

    fun helperRequestDisabled(client: ClientConnection) =
        send(client, CallForHelpDisabledNotifyOutCodec, CallForHelpDisabledNotifyOut(""))

    fun bullyReported(
        client: ClientConnection,
        code: Int,
    ) = send(client, GuideTicketCreationResultOutCodec, GuideTicketCreationResultOut(code))

    fun toolWindow(
        client: ClientConnection,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) = send(client, ModeratorToolPreferencesOutCodec, ModeratorToolPreferencesOut(x, y, width, height))

    fun issueInfo(
        client: ClientConnection,
        issue: SupportTicket,
        now: Int,
    ) = send(client, IssueInfoOutCodec, ModerationMessages.issueInfo(issue, now))

    fun issuePickFailed(
        client: ClientConnection,
        issues: List<SupportTicket>,
        retryEnabled: Boolean,
        retryCount: Int,
    ) = send(
        client,
        IssuePickFailedOutCodec,
        ModerationMessages.issuePickFailed(issues, retryEnabled, retryCount),
    )
}
