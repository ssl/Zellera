package zel.room.furni.buildpass

import zel.room.RoomRegistry

class BuildPassRoomSync(
    private val registry: RoomRegistry,
    private val store: BuildPassStore,
    private val lock: BuildPassRoomLock,
) {
    fun ofOwner(ownerId: Int): Int {
        val member = store.account(ownerId).isMember
        return store.ownedRooms(ownerId).count { sync(it, member) }
    }

    private fun sync(
        room: BuildPassOwnedRoom,
        member: Boolean,
    ): Boolean {
        val lockIt = room.hasFurni && !member
        if (registry.loaded(room.id) != null) {
            registry.post(room.id) { scope ->
                if (lock.sync(scope, member) != BuildPassLockChange.None) BuildPassOut.roomSettings(scope)
            }
        } else if (lockIt && !(room.locked && room.hidden)) {
            store.lockRoom(room.id)
        } else if (!lockIt && room.locked) {
            store.unlockRoom(room.id)
        }
        return lockIt != room.locked
    }
}
