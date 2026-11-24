package zel.room.entry

data class Pending(
    val card: PlayerCard,
    val visitor: Visitor,
    val room: RoomCard,
    val facts: EntryFacts,
)
