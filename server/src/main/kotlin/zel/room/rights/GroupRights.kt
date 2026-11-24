package zel.room.rights

import zel.room.entry.EntryStore
import zel.room.entry.GroupLevels

class GroupRights(
    private val store: EntryStore,
) {
    fun decorates(
        groupId: Int,
        playerId: Int,
    ): Boolean {
        val group = store.group(groupId) ?: return false
        return when (store.memberLevel(group.id, playerId)) {
            GroupLevels.OWNER, GroupLevels.ADMIN -> true
            GroupLevels.MEMBER -> group.membersDecorate
            else -> false
        }
    }
}
