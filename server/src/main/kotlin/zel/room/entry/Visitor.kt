package zel.room.entry

data class Visitor(
    val level: Int,
    val entersAnyRoom: Boolean,
    val entersFullRooms: Boolean,
    val banned: Boolean,
    val groupMember: Boolean = false,
)
