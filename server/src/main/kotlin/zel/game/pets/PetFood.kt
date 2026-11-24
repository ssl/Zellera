package zel.game.pets

import zel.game.items.Item

/** Food params open with energy*10 per bite, the whole food's experience and its treat, shared over its bites. */
object PetFood {
    const val PLAIN_BITE_ENERGY = 34

    const val PLAIN_BITE_EXPERIENCE = 2

    private const val ENERGY_PER_NUMBER = 10
    private const val NUMBERS = 3

    /** Null when the parameters open with something else. */
    data class Nutrition(
        val energy: Int,
        val experience: Int,
        val treat: Int,
    )

    fun nutrition(food: Item): Nutrition? =
        food.params
            .substringBefore(' ')
            .split(',')
            .takeIf { it.size == NUMBERS }
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.takeIf { it.size == NUMBERS && it.any { n -> n > 0 } }
            ?.let { Nutrition(it[0], it[1], it[2]) }

    /** A bowl is eaten up at its last state, so it holds one bite more than it has states after the first. */
    fun bites(food: Item): Int = maxOf(1, food.stateCount + 1)

    fun biteEnergy(food: Item): Int =
        nutrition(food)?.let { (it.energy + ENERGY_PER_NUMBER / 2) / ENERGY_PER_NUMBER + it.treat / bites(food) }
            ?: PLAIN_BITE_ENERGY

    fun biteExperience(food: Item): Int = nutrition(food)?.let { it.experience / bites(food) } ?: PLAIN_BITE_EXPERIENCE

    fun biteHappiness(food: Item): Int = nutrition(food)?.let { it.treat / bites(food) } ?: 0

    fun wantsFood(
        energy: Int,
        maxEnergy: Int,
    ): Boolean = energy < maxEnergy

    fun foodLeft(
        state: Int,
        stateCount: Int,
    ): Boolean = state <= stateCount

    fun lastBite(
        state: Int,
        stateCount: Int,
    ): Boolean = state >= stateCount
}
