package zel.protocol.outgoing.guilds.forums

import zel.game.guilds.forums.ForumPermissions
import zel.game.guilds.forums.ForumSummary
import zel.game.guilds.forums.MessageData
import zel.game.guilds.forums.ThreadData
import zel.net.clients.ClientConnection
import zel.protocol.generated.ForumDataOut
import zel.protocol.generated.ForumDataOutCodec
import zel.protocol.generated.ForumThreadsOut
import zel.protocol.generated.ForumThreadsOutCodec
import zel.protocol.generated.ForumsListOut
import zel.protocol.generated.ForumsListOutCodec
import zel.protocol.generated.PostMessageOut
import zel.protocol.generated.PostMessageOutCodec
import zel.protocol.generated.PostThreadOut
import zel.protocol.generated.PostThreadOutCodec
import zel.protocol.generated.ThreadMessagesOut
import zel.protocol.generated.ThreadMessagesOutCodec
import zel.protocol.generated.UnreadForumsCountOut
import zel.protocol.generated.UnreadForumsCountOutCodec
import zel.protocol.generated.UpdateMessageOut
import zel.protocol.generated.UpdateMessageOutCodec
import zel.protocol.generated.UpdateThreadOut
import zel.protocol.generated.UpdateThreadOutCodec

class ForumMessages {
    fun sendForumData(
        client: ClientConnection,
        summary: ForumSummary,
        permissions: ForumPermissions,
    ) = client.send(
        ForumDataOutCodec,
        ForumDataOut(
            summary.guildId,
            summary.name,
            summary.description,
            summary.badge,
            summary.totalThreads,
            0,
            summary.totalComments,
            summary.newComments,
            summary.lastCommentId,
            summary.lastAuthorId,
            summary.lastAuthorName,
            summary.lastCommentAgeSeconds,
            permissions.readLevel,
            permissions.postMessagesLevel,
            permissions.postThreadsLevel,
            permissions.moderateLevel,
            permissions.readError,
            permissions.postError,
            permissions.startThreadError,
            permissions.moderateError,
            "",
            permissions.canChangeSettings,
            permissions.canModerate,
        ),
    )

    fun sendUnreadableForumData(
        client: ClientConnection,
        guildId: Int,
        readError: String,
        now: Int,
    ) = client.send(
        ForumDataOutCodec,
        ForumDataOut(
            guildId,
            "",
            "",
            "",
            0,
            0,
            0,
            0,
            0,
            0,
            "n/a",
            now,
            0,
            0,
            0,
            0,
            readError,
            readError,
            readError,
            readError,
            readError,
            false,
            false,
        ),
    )

    fun sendForumList(
        client: ClientConnection,
        mode: Int,
        total: Int,
        index: Int,
        forums: List<ForumSummary>,
    ) = client.send(
        ForumsListOutCodec,
        ForumsListOut(
            mode,
            total,
            index,
            forums.map {
                ForumsListOut.List1Item(
                    it.guildId,
                    it.name,
                    it.description,
                    it.badge,
                    it.totalThreads,
                    0,
                    it.totalComments,
                    it.newComments,
                    it.lastCommentId,
                    it.lastAuthorId,
                    it.lastAuthorName,
                    it.lastCommentAgeSeconds,
                )
            },
        ),
    )

    fun sendThreads(
        client: ClientConnection,
        guildId: Int,
        index: Int,
        threads: List<ThreadData>,
    ) = client.send(
        ForumThreadsOutCodec,
        ForumThreadsOut(
            guildId,
            index,
            threads.map {
                ForumThreadsOut.List1Item(
                    it.threadId,
                    it.openerId,
                    it.openerName,
                    it.subject,
                    it.pinned,
                    it.locked,
                    it.ageSeconds,
                    it.totalComments,
                    it.newComments,
                    it.lastMessageId,
                    it.lastAuthorId,
                    it.lastAuthorName,
                    it.lastMessageAgeSeconds,
                    it.stateId,
                    it.adminId,
                    it.adminName,
                    it.adminOperationValue,
                )
            },
        ),
    )

    fun sendNewThread(
        client: ClientConnection,
        guildId: Int,
        t: ThreadData,
    ) = client.send(
        PostThreadOutCodec,
        PostThreadOut(
            guildId,
            t.threadId,
            t.openerId,
            t.openerName,
            t.subject,
            t.pinned,
            t.locked,
            t.ageSeconds,
            t.totalComments,
            t.newComments,
            t.lastMessageId,
            t.lastAuthorId,
            t.lastAuthorName,
            t.lastMessageAgeSeconds,
            t.stateId,
            t.adminId,
            t.adminName,
            t.adminOperationValue,
        ),
    )

    fun sendUpdatedThread(
        client: ClientConnection,
        guildId: Int,
        t: ThreadData,
    ) = client.send(
        UpdateThreadOutCodec,
        UpdateThreadOut(
            guildId,
            t.threadId,
            t.openerId,
            t.openerName,
            t.subject,
            t.pinned,
            t.locked,
            t.ageSeconds,
            t.totalComments,
            t.newComments,
            t.lastMessageId,
            t.lastAuthorId,
            t.lastAuthorName,
            t.lastMessageAgeSeconds,
            t.stateId,
            t.adminId,
            t.adminName,
            t.adminOperationValue,
        ),
    )

    fun sendMessages(
        client: ClientConnection,
        guildId: Int,
        threadId: Int,
        index: Int,
        messages: List<MessageData>,
    ) = client.send(
        ThreadMessagesOutCodec,
        ThreadMessagesOut(
            guildId,
            threadId,
            index,
            messages.map {
                ThreadMessagesOut.List1Item(
                    it.messageId,
                    it.index,
                    it.authorId,
                    it.authorName,
                    it.authorLook,
                    it.ageSeconds,
                    it.text,
                    it.stateId,
                    it.adminId,
                    it.adminName,
                    it.adminOperationValue,
                    it.authorPostCount,
                )
            },
        ),
    )

    fun sendNewMessage(
        client: ClientConnection,
        guildId: Int,
        threadId: Int,
        m: MessageData,
    ) = client.send(
        PostMessageOutCodec,
        PostMessageOut(
            guildId,
            threadId,
            m.messageId,
            m.index,
            m.authorId,
            m.authorName,
            m.authorLook,
            m.ageSeconds,
            m.text,
            m.stateId,
            m.adminId,
            m.adminName,
            m.adminOperationValue,
            m.authorPostCount,
        ),
    )

    fun sendUpdatedMessage(
        client: ClientConnection,
        guildId: Int,
        threadId: Int,
        m: MessageData,
    ) = client.send(
        UpdateMessageOutCodec,
        UpdateMessageOut(
            guildId,
            threadId,
            m.messageId,
            m.index,
            m.authorId,
            m.authorName,
            m.authorLook,
            m.ageSeconds,
            m.text,
            m.stateId,
            m.adminId,
            m.adminName,
            m.adminOperationValue,
            m.authorPostCount,
        ),
    )

    fun sendUnreadCount(
        client: ClientConnection,
        count: Int,
    ) = client.send(UnreadForumsCountOutCodec, UnreadForumsCountOut(count))
}
