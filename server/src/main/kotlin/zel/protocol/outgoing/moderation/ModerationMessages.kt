package zel.protocol.outgoing.moderation

import zel.game.moderation.FiledReport
import zel.game.moderation.MySanctionStatus
import zel.game.moderation.RoomVisit
import zel.game.moderation.SupportTicket
import zel.protocol.generated.CallForHelpPendingCallsOut
import zel.protocol.generated.CfhTopicsInitOut
import zel.protocol.generated.GuideReportingStatusOut
import zel.protocol.generated.IssueInfoOut
import zel.protocol.generated.IssuePickFailedOut
import zel.protocol.generated.ModeratorInitOut
import zel.protocol.generated.MyCfhReportStatusOut
import zel.protocol.generated.MySanctionStatusOut
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.RoomVisitsOut
import zel.util.GameZone
import java.time.Instant

object ModerationMessages {
    private const val MAX_QUEUED_ISSUES = 100

    fun issueInfo(
        issue: SupportTicket,
        now: Int,
    ): IssueInfoOut =
        IssueInfoOut(
            int1 = issue.id,
            int2 = issue.state.wire,
            int3 = issue.type.code,
            int4 = issue.category,
            int5 = now - issue.createdAt,
            int6 = issue.priority,
            int7 = 1,
            int8 = issue.reporterId,
            string1 = issue.reporterName.orEmpty(),
            int9 = issue.accusedId,
            string2 = issue.accusedName.orEmpty(),
            int10 = issue.moderatorId,
            string3 = issue.moderatorName.orEmpty(),
            string4 = issue.message,
            int11 = 0,
            list1 = emptyList(),
        )

    private fun initItem(
        issue: SupportTicket,
        now: Int,
    ): ModeratorInitOut.List1Item {
        val info = issueInfo(issue, now)
        return ModeratorInitOut.List1Item(
            int1 = info.int1,
            int2 = info.int2,
            int3 = info.int3,
            int4 = info.int4,
            int5 = info.int5,
            int6 = info.int6,
            int7 = info.int7,
            int8 = info.int8,
            string1 = info.string1,
            int9 = info.int9,
            string2 = info.string2,
            int10 = info.int10,
            string3 = info.string3,
            string4 = info.string4,
            int11 = info.int11,
            list1 = emptyList(),
        )
    }

    fun moderatorInit(
        openIssues: Collection<SupportTicket>,
        canSeeQueue: Boolean,
        userPresets: Collection<String>,
        categoryNames: Collection<String>,
        rights: ModeratorRights,
        roomPresets: Collection<String>,
        now: Int,
    ): ModeratorInitOut =
        ModeratorInitOut(
            list1 = if (canSeeQueue) openIssues.take(MAX_QUEUED_ISSUES).map { initItem(it, now) } else emptyList(),
            list2 = userPresets.toList(),
            list3 = categoryNames.toList(),
            boolean1 = rights.ticketQueue,
            boolean2 = rights.userLogs,
            boolean3 = rights.userAlert,
            boolean4 = rights.userKick,
            boolean5 = rights.userBan,
            boolean6 = rights.roomInfo,
            boolean7 = rights.roomLogs,
            list4 = roomPresets.toList(),
        )

    /** Flags in wire order. */
    data class ModeratorRights(
        val ticketQueue: Boolean,
        val userLogs: Boolean,
        val userAlert: Boolean,
        val userKick: Boolean,
        val userBan: Boolean,
        val roomInfo: Boolean,
        val roomLogs: Boolean,
    )

    fun issuePickFailed(
        issues: List<SupportTicket>,
        retryEnabled: Boolean,
        retryCount: Int,
    ): IssuePickFailedOut =
        IssuePickFailedOut(
            list1 = issues.map { IssuePickFailedOut.List1Item(it.id, it.moderatorId, it.moderatorName.orEmpty()) },
            boolean1 = retryEnabled,
            int1 = retryCount,
        )

    fun pendingCalls(issues: List<SupportTicket>): CallForHelpPendingCallsOut =
        CallForHelpPendingCallsOut(
            issues.map {
                CallForHelpPendingCallsOut.List1Item(
                    it.id.toString(),
                    it.createdAt.toString(),
                    it.message,
                )
            },
        )

    fun cfhTopics(categories: Collection<CfhCategoryView>): CfhTopicsInitOut =
        CfhTopicsInitOut(
            categories.map { category ->
                CfhTopicsInitOut.List1Item(
                    category.name,
                    category.topics.map { CfhTopicsInitOut.List1Item.List1Item(it.name, it.id, it.consequence) },
                )
            },
        )

    data class CfhCategoryView(
        val name: String,
        val topics: List<CfhTopicView>,
    )

    data class CfhTopicView(
        val name: String,
        val id: Int,
        val consequence: String,
    )

    fun myReportsStatus(reports: List<FiledReport>): MyCfhReportStatusOut =
        MyCfhReportStatusOut(
            reports.map {
                MyCfhReportStatusOut.List1Item(
                    long1 = it.id.toLong(),
                    long2 = it.createdAt,
                    string1 = it.message,
                    int1 = it.category,
                    string2 = it.reportedName,
                    long3 = it.closedAt,
                    boolean1 = it.sanctioned,
                    boolean2 = it.sanctionedAutomatically,
                    byte1 = it.appeal.code.toByte(),
                    long4 = it.appealedAt,
                    long5 = it.appealResolvedAt,
                )
            },
        )

    fun mySanctionStatus(entries: List<MySanctionStatus.Entry>): MySanctionStatusOut =
        MySanctionStatusOut(
            entries.map {
                val current = it.current
                val next = it.next
                MySanctionStatusOut.List1Item(
                    string1 = current.name,
                    int1 = current.hours,
                    int2 = current.probationDays,
                    string2 = it.text,
                    boolean1 = it.gradual,
                    int3 = it.probationHoursLeft,
                    string3 = next.name,
                    int4 = next.hours,
                    int5 = next.probationDays,
                )
            },
        )

    fun roomVisits(
        userId: Int,
        username: String,
        visits: Collection<RoomVisit>,
    ): RoomVisitsOut {
        val zone = GameZone.current
        return RoomVisitsOut(
            int1 = userId,
            string1 = username,
            list1 =
                visits.map {
                    val at = Instant.ofEpochSecond(it.timestamp.toLong()).atZone(zone)
                    RoomVisitsOut.List1Item(it.roomId, it.roomName, at.hour, at.minute)
                },
        )
    }

    fun guideReportingStatus(
        code: Int,
        pendingCaseType: Int,
    ): GuideReportingStatusOut {
        if (code != ONGOING_HELPER_CASE) return GuideReportingStatusOut(code, ByteArray(0))
        val tail = PacketWriter().int(pendingCaseType).int(1)
        tail.boolean(pendingCaseType != TYPE_ROOM_REPORT)
        if (pendingCaseType != TYPE_ROOM_REPORT) {
            tail.string("").string("")
            if (pendingCaseType == TYPE_BULLY_REPORT) tail.string("")
        }
        return GuideReportingStatusOut(code, tail.toByteArray())
    }

    const val ONGOING_HELPER_CASE = 1
    const val TYPE_BULLY_REPORT = 1
    const val TYPE_ROOM_REPORT = 3
}
