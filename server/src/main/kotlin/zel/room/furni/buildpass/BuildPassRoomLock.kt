package zel.room.furni.buildpass

import zel.core.rules.rules
import zel.room.DoorMode
import zel.room.RoomScope
import zel.room.RoomWrites

class BuildPassRoomLock(
    private val store: BuildPassStore,
    private val writes: RoomWrites,
) {
    /** Only furni from the owner's own pool count: staff pay for theirs elsewhere. */
    fun sync(
        scope: RoomScope,
        ownerIsMember: Boolean,
        placing: Boolean = false,
    ): BuildPassLockChange {
        val room = scope.room
        val info = room.info
        val roomId = info.id
        val hasFurni = placing || room.furni.all().any { it.isBuildPass && it.placerId == info.ownerId }
        val locked = info.buildPassDoor
        return when {
            rules.roomLock && hasFurni && !ownerIsMember -> {
                if (locked == null || info.door != DoorMode.Invisible) {
                    room.updateInfo(info.copy(door = DoorMode.Invisible, buildPassDoor = locked ?: info.door))
                    writes.write(roomId, Runnable { store.lockRoom(roomId) })
                }
                if (locked == null) BuildPassLockChange.Locked else BuildPassLockChange.None
            }

            locked != null -> {
                room.updateInfo(info.copy(door = locked, buildPassDoor = null))
                writes.write(roomId, Runnable { store.unlockRoom(roomId) })
                BuildPassLockChange.Unlocked
            }

            else -> {
                BuildPassLockChange.None
            }
        }
    }
}
