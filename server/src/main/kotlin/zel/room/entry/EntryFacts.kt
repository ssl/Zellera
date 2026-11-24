package zel.room.entry

data class EntryFacts(
    val group: RoomGroup?,
    val isGroupMember: Boolean,
    val promotion: RoomPromotion?,
    val voted: Boolean,
)
