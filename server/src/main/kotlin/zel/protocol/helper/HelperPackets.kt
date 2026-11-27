package zel.protocol.helper

import zel.core.Configuration
import zel.core.rules.rules
import zel.game.moderation.ModerationService
import zel.game.moderation.MySanctionStatus
import zel.game.moderation.Sanctions
import zel.net.clients.ClientConnection
import zel.protocol.generated.AppealCfhInCodec
import zel.protocol.generated.GetCfhMyReportStatusInCodec
import zel.protocol.generated.GetMySanctionStatusInCodec
import zel.protocol.generated.MyCfhReportStatusOutCodec
import zel.protocol.generated.MySanctionStatusOutCodec
import zel.protocol.outgoing.moderation.ModerationMessages
import zel.protocol.outgoing.moderation.ModerationReplies
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute

@Suppress("ReturnCount") // failed checks drop the request
class HelperPackets(
    private val configuration: Configuration,
    private val sanctions: Sanctions,
    private val moderation: ModerationService,
    private val clock: () -> Int,
) : PacketPackage {
    override val name = "helper"

    private val appealCooldown = ClientCooldown("helper.appeal", APPEAL_COOLDOWN)
    private val reportsCooldown = ClientCooldown("helper.reports", STATUS_COOLDOWN)
    private val sanctionsCooldown = ClientCooldown("helper.sanctions", STATUS_COOLDOWN)

    override val routes =
        listOf(
            PacketRoute(AppealCfhInCodec) { session, m -> appealReport(session.client, m.reportId) },
            PacketRoute(GetCfhMyReportStatusInCodec) { session, _ -> reportsStatus(session.client) },
            PacketRoute(GetMySanctionStatusInCodec) { session, _ -> sanctionStatus(session.client) },
        )

    private fun appealReport(
        client: ClientConnection,
        reportId: Int,
    ) {
        if (!appealCooldown.acquire(client)) return
        if (reportId <= 0) return
        val userId = checkNotNull(client.player).profile.id
        val report = moderation.myReports.ofReporter(userId, reportId)
        if (report == null || !report.appealable) return
        if (moderation.tickets.filedBy(userId).size >= rules.limits.reportsOpenPerPlayer) return
        if (!moderation.myReports.appeal(userId, reportId, clock())) return
        moderation.tickets.reopenForAppeal(reportId)
        ModerationReplies.send(
            client,
            MyCfhReportStatusOutCodec,
            ModerationMessages.myReportsStatus(moderation.myReports.ofReporter(userId)),
        )
    }

    private fun reportsStatus(client: ClientConnection) {
        if (!reportsCooldown.acquire(client)) return
        ModerationReplies.send(
            client,
            MyCfhReportStatusOutCodec,
            ModerationMessages.myReportsStatus(
                moderation.myReports.ofReporter(checkNotNull(client.player).profile.id),
            ),
        )
    }

    private fun sanctionStatus(client: ClientConnection) {
        if (!sanctionsCooldown.acquire(client)) return
        val player = client.player ?: return
        val ladder = configuration.getBoolean("moderation.sanction_ladder.is_on")
        val userId = player.profile.id
        ModerationReplies.send(
            client,
            MySanctionStatusOutCodec,
            ModerationMessages.mySanctionStatus(
                MySanctionStatus.entries(
                    if (ladder) sanctions.latestOf(userId) else null,
                    sanctions::step,
                    clock(),
                    player.stats.standing.muteSecondsLeft(),
                    moderation::gameText,
                ),
            ),
        )
    }

    private companion object {
        const val APPEAL_COOLDOWN = 2000L
        const val STATUS_COOLDOWN = 1000L
    }
}
