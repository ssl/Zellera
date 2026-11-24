package zel.room.furni.buildpass

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.DeadlockRetry
import zel.room.RoomScope
import zel.room.RoomWrites
import zel.room.furni.RoomFurni

class BuildPassPickups(
    private val reader: BuildPassReader,
    private val store: BuildPassStore,
    private val writes: RoomWrites,
) : BuildPassPicks {
    private val lock = BuildPassRoomLock(store, writes)

    override fun before(furniId: Int): BuildPassPickup? = reader.pickup(furniId)

    override fun counts(
        scope: RoomScope,
        before: BuildPassPickup,
    ) {
        if (lock.sync(scope, before.roomOwnerIsMember) == BuildPassLockChange.Unlocked) {
            BuildPassOut.notice(scope, scope.room.info.ownerId, ROOM_UNLOCKED)
            BuildPassOut.roomSettings(scope)
        }
        before.accounts.firstOrNull()?.let {
            BuildPassOut.count(scope, it.playerId, it.furniCount - 1)
            BuildPassOut.status(scope, it)
        }
        before.accounts
            .drop(1)
            .filter { scope.room.units.ofPlayer(it.playerId) != null }
            .forEach {
                BuildPassOut.count(scope, it.playerId, it.furniCount)
                BuildPassOut.status(scope, it)
            }
    }

    override fun swept(
        scope: RoomScope,
        befores: List<BuildPassPickup>,
    ) {
        val last = befores.lastOrNull() ?: return
        if (lock.sync(scope, last.roomOwnerIsMember) == BuildPassLockChange.Unlocked) {
            BuildPassOut.notice(scope, scope.room.info.ownerId, ROOM_UNLOCKED)
            BuildPassOut.roomSettings(scope)
        }
        val byPlacer = befores.filter { it.accounts.isNotEmpty() }.groupBy { it.accounts.first().playerId }
        byPlacer.values.forEach { picks ->
            val placer = picks.first().accounts.first()
            BuildPassOut.count(scope, placer.playerId, placer.furniCount - picks.size)
            BuildPassOut.status(scope, placer)
        }
        befores
            .flatMap { it.accounts.drop(1) }
            .distinctBy { it.playerId }
            .filter { it.playerId !in byPlacer && scope.room.units.ofPlayer(it.playerId) != null }
            .forEach {
                BuildPassOut.count(scope, it.playerId, it.furniCount)
                BuildPassOut.status(scope, it)
            }
    }

    @Suppress("TooGenericExceptionCaught") // logged; the furni stays out
    override fun gone(
        scope: RoomScope,
        furni: RoomFurni,
        before: BuildPassPickup?,
    ) {
        val rowId = furni.rowId
        val roomId = scope.room.info.id
        writes.write(
            roomId,
            Runnable {
                try {
                    DeadlockRetry.run { store.delete(rowId) }
                } catch (e: Exception) {
                    log.error("room {}: furni {} failed to go back to the Build Pass", roomId, rowId, e)
                }
            },
        )
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(BuildPassPickups::class.java)

        const val ROOM_UNLOCKED = "build_pass.room_unlocked"
    }
}
