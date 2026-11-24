package zel.game.pets

import java.sql.ResultSet

data class PetStats(
    val happiness: Int,
    val experience: Int,
    val energy: Int,
    val respect: Int,
    val thirst: Int,
) {
    fun withEnergy(
        change: Int,
        max: Int,
    ): PetStats =
        when {
            change > 0 && energy >= max -> this
            change > 0 -> copy(energy = minOf(max, energy + change))
            else -> copy(energy = maxOf(0, energy + change))
        }

    /** A bite adds its energy above the maximum too. */
    fun withBite(biteEnergy: Int): PetStats = copy(energy = energy + biteEnergy)

    fun withHappiness(change: Int): PetStats = copy(happiness = (happiness + change).coerceIn(0, MAX_HAPPINESS))

    fun withThirst(change: Int): PetStats = copy(thirst = (thirst + change).coerceIn(0, MAX_NEED))

    fun respected(): PetStats = copy(respect = respect + 1)

    companion object {
        const val MAX_HAPPINESS = 100
        const val MAX_NEED = 100

        val NEW: PetStats = PetStats(MAX_HAPPINESS, 0, PetLevels.maxEnergyAt(1), 0, 0)

        fun stored(set: ResultSet): PetStats =
            PetStats(
                set.getInt("happiness"),
                set.getInt("experience"),
                set.getInt("energy"),
                set.getInt("respect"),
                set.getInt("thirst"),
            )
    }
}
