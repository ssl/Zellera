package zel.room.unit.pet

data class PetTuning(
    val needs: Needs = Needs(),
    val moods: Moods = Moods(),
    val obedience: Obedience = Obedience(),
    val plants: Plants = Plants(),
) {
    /** Thirst and energy change by the clock; a walking pet loses a happiness every [walkingSadnessMillis]. */
    data class Needs(
        val restEnergyDivisor: Int = 64,
        val thirstMillis: Long = 36_000L,
        val walkingSadnessMillis: Long = 30_000L,
    )

    /** A pet below a [wearyEnergyDivisor]th of its energy looks for food or rest. */
    data class Moods(
        val everyMillis: Long = 15_000L,
        val gestureMillis: Long = 5_000L,
        val wearyEnergyDivisor: Int = 4,
        val thirstyFrom: Int = 80,
        val happyFromHappiness: Int = 90,
        val sadUpToHappiness: Int = 5,
    )

    /** Rare seeds need [rareParentFrom] rarity on both parents; two owners get one seed in [oneSeedPercent]. */
    data class Plants(
        val rareParentFrom: Int = 8,
        val oneSeedPercent: Int = 50,
    )

    /**
     * Below a [drainedEnergyDivisor]th of its energy a pet obeys in [drainedPercent], else in [restedPercent]; under
     * [sulksBelowHappiness] it obeys nothing. A command within [gapMillis] of the last one is not heard, nor the same
     * one more than [repeatsAllowed] times running inside [repeatWindowMillis].
     */
    data class Obedience(
        val restedPercent: Int = 75,
        val drainedPercent: Int = 12,
        val drainedEnergyDivisor: Int = 8,
        val sulksBelowHappiness: Int = 10,
        val gapMillis: Long = 2_000L,
        val repeatWindowMillis: Long = 10_000L,
        val repeatsAllowed: Int = 3,
        val grumbleOneIn: Int = 3,
        val stayMillis: Long = 120_000L,
    )
}
