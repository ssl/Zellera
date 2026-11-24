package zel.game.pets

object PetLevels {
    val experienceGoals =
        intArrayOf(
            100,
            200,
            400,
            600,
            900,
            1300,
            1800,
            2400,
            3200,
            4300,
            5700,
            7600,
            10100,
            13300,
            17500,
            23000,
            30200,
            39600,
            51900,
        )

    private const val BASE_ENERGY = 100
    private const val ENERGY_PER_LEVEL = 20

    fun levelFor(experience: Int): Int {
        val goalsPassed = experienceGoals.indexOfFirst { it > experience }
        return (if (goalsPassed == -1) experienceGoals.size else goalsPassed) + 1
    }

    fun goalAt(level: Int): Int = experienceGoals[(minOf(level, experienceGoals.size) - 1).coerceAtLeast(0)]

    fun maxEnergyAt(level: Int): Int = BASE_ENERGY + ENERGY_PER_LEVEL * level
}
