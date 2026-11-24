package zel.room.furni.buildpass

import zel.room.RoomInfo
import zel.room.furni.BuildPass
import zel.room.furni.place.FurniAccess

/** Reads the numbers a placement or pickup needs, on the packet thread, before the room's lock. */
class BuildPassReader(
    private val store: BuildPassStore,
) {
    fun placer(
        access: FurniAccess,
        info: RoomInfo,
        name: String,
        hasClub: Boolean,
    ): BuildPassPlacer {
        val id = access.playerId
        val own = store.account(id)
        val groupAdmins = if (info.groupId > 0) store.groupAdmins(info.groupId) else emptyList()
        val owner = if (info.ownerId == id) own else store.account(info.ownerId)
        val poolId =
            when {
                access.isRoomOwner || access.staffPlaces -> id
                id in groupAdmins && own.isMember && owner.isMember -> info.ownerId
                else -> null
            }
        val pool = poolId?.let { if (it == id) own else store.account(it) }
        return BuildPassPlacer(
            access,
            hasClub,
            name,
            own,
            pool,
            if (poolId == info.ownerId) info.ownerName else name,
            if (poolId == info.ownerId) members(groupAdmins, poolId) else emptyList(),
            owner.isMember,
        )
    }

    fun pickup(furniId: Int): BuildPassPickup? {
        if (!BuildPass.isClientId(furniId)) return null
        val tracked = store.tracked(furniId) ?: return null
        val admins =
            if (tracked.groupId >
                0
            ) {
                members(store.groupAdmins(tracked.groupId), tracked.placerId)
            } else {
                emptyList()
            }
        val placer = store.account(tracked.placerId)
        return BuildPassPickup(listOf(placer) + admins, store.account(tracked.ownerId).isMember)
    }

    private fun members(
        players: List<Int>,
        except: Int,
    ): List<BuildPassAccount> = players.filter { it != except }.map(store::account).filter { it.isMember }
}
