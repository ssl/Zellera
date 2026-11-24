package zel.wired.chest

import zel.content.ClientStrings
import zel.protocol.generated.NotificationDialogOut
import zel.protocol.generated.NotificationDialogOutCodec
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.util.MILLIS_PER_SECOND

class ChestNotices(
    private val chests: RoomChests,
    private val store: ChestStore,
    private val host: ChestHost,
    private val persist: (Runnable) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun tell(
        scope: RoomScope,
        ownerId: Int,
        chest: Chest,
        notice: ChestNotice,
        actorId: Int,
        actorName: String,
    ) {
        val silent =
            ownerId == actorId ||
                !chest.notifications.wants(notice) ||
                (chest.notifications.onlyWhileAway && scope.room.units.ofPlayer(ownerId) != null)
        if (!silent) deliver(scope, ownerId, chest, notice, actorId, actorName)
    }

    fun earnings(
        scope: RoomScope,
        ownerId: Int,
        chest: Chest,
    ) = deliver(scope, ownerId, chest, ChestNotice.EARNINGS, 0, "")

    private fun deliver(
        scope: RoomScope,
        ownerId: Int,
        chest: Chest,
        notice: ChestNotice,
        actorId: Int,
        actorName: String,
    ) {
        val roomId = scope.room.info.id
        if (host.isOnline(ownerId)) {
            host.send(ownerId, NotificationDialogOutCodec, dialog(notice, actorName, roomId, chest.preferences.name))
        } else {
            val pending =
                PendingNotice(ownerId, chest.id, roomId, notice.reason, chest.preferences.name, actorId, seconds())
            persist(Runnable { store.addPending(pending) })
        }
    }

    private fun seconds(): Int = (clock() / MILLIS_PER_SECOND).toInt()

    fun failureToRoom(scope: RoomScope) {
        if (!chests.takeFailureDialog(scope.room, clock())) return
        scope.out.send(Lane.Now, Audience.Room, NotificationDialogOutCodec, FAILURE)
    }

    fun failureToOwner(scope: RoomScope) = failureTo(scope, scope.room.info.ownerId)

    fun failureTo(
        scope: RoomScope,
        playerId: Int,
    ) {
        if (!chests.takeFailureDialog(scope.room, clock())) return
        scope.out.send(Lane.Now, Audience.Player(playerId), NotificationDialogOutCodec, FAILURE)
    }

    companion object {
        fun dialog(
            notice: ChestNotice,
            actorName: String,
            roomId: Int,
            chestName: String,
        ): NotificationDialogOut {
            val keys =
                when (notice) {
                    ChestNotice.EARNINGS -> {
                        emptyList()
                    }

                    ChestNotice.DONATION, ChestNotice.WITHDRAW -> {
                        listOf(
                            USER_NAME to actorName,
                            LINK_URL to "$FORWARD$roomId",
                            CHEST_NAME to chestName,
                        )
                    }

                    else -> {
                        listOf(LINK_URL to "$FORWARD$roomId", CHEST_NAME to chestName)
                    }
                }
            return NotificationDialogOut(
                "$PREFIX${notice.kind}",
                keys.map { NotificationDialogOut.List1Item(it.first, it.second) },
            )
        }

        private const val PREFIX = "wired_chests."
        private const val USER_NAME = "user_name"
        private const val LINK_URL = "linkUrl"
        private const val CHEST_NAME = "chest_name"
        private const val FORWARD = "navigator/ask_forward/"

        val FAILURE =
            NotificationDialogOut(
                "wired.error",
                listOf(
                    NotificationDialogOut.List1Item("image", ClientStrings.badgeImage("WIRD2")),
                    NotificationDialogOut.List1Item(
                        "message",
                        "\${room.error.wired.transaction_failure}",
                    ),
                ),
            )
    }
}
