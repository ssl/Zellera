package zel.room.furni

import zel.room.furni.place.Interactions

object StoredValues {
    fun holds(furni: RoomFurni): Boolean = holds(Interactions.of(furni.definition))

    fun holds(type: String): Boolean = type.startsWith(CRACKABLE) || type in TYPES

    private const val CRACKABLE = "crack."

    private val TYPES =
        setOf(
            "lock.love",
            "display.trophy",
            "display.badge",
            "area.rental",
            "outfit.mannequin",
            "game.football.gate",
            "gift.box",
            Interactions.POST_IT,
            "display.image",
            "pet.seed",
            "plant.pot",
        )
}
