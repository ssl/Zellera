package zel.room.unit.path

data class Mover(
    val index: Int,
    val ignoredUnits: Set<Int> = setOf(index),
    val skater: Boolean = false,
    val horse: HorseRole = HorseRole.None,
    val letThrough: Set<Int> = emptySet(),
    val nests: Set<Int> = emptySet(),
    val playerId: Int? = null,
) {
    fun mayUse(
        passage: Passage,
        asGoal: Boolean,
    ): Boolean =
        when (passage) {
            Passage.SkateRail -> {
                skater
            }

            is Passage.Obstacle -> {
                when (horse) {
                    HorseRole.None -> false
                    HorseRole.Ridden -> !(asGoal && passage.middle)
                }
            }

            is Passage.Transit -> {
                passage.furniId in letThrough
            }

            is Passage.Nest -> {
                passage.furniId in nests
            }

            is Passage.Rented -> {
                passage.renterId == playerId
            }
        }
}
