package zel.room.rights

import zel.room.RoomInfo
import zel.room.entry.DoorCheck

/** Whom each mute/kick/ban setting lets act: a rights level or owner and up; group admins only in group rooms. */
object ModerationSetting {
    const val OWNER_ONLY = 0
    const val RIGHTS = 1
    const val EVERYONE = 2
    const val GROUP_ADMINS = 4
    const val GROUP_ADMINS_AND_RIGHTS = 5

    fun allows(
        setting: Int,
        level: Int,
        groupId: Int,
    ): Boolean {
        val rights = level == DoorCheck.RIGHTS || level >= DoorCheck.OWNER
        val groupAdmin = groupId != 0 && level >= DoorCheck.GROUP_ADMIN
        return when (setting) {
            RIGHTS -> rights
            GROUP_ADMINS -> groupAdmin
            GROUP_ADMINS_AND_RIGHTS -> rights || groupAdmin
            else -> level >= DoorCheck.OWNER
        }
    }

    fun mayMute(
        level: Int,
        room: RoomInfo,
    ): Boolean = allows(room.moderation.whoCanMute, level, room.groupId)

    fun mayKick(
        level: Int,
        room: RoomInfo,
    ): Boolean = room.moderation.whoCanKick == EVERYONE || allows(room.moderation.whoCanKick, level, room.groupId)

    fun mayBan(
        level: Int,
        room: RoomInfo,
    ): Boolean = allows(room.moderation.whoCanBan, level, room.groupId)
}
