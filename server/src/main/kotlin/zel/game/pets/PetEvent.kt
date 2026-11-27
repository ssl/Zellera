package zel.game.pets

sealed interface PetEvent {
    data class LevelGained(
        val pet: Pet,
    ) : PetEvent

    data class Scratched(
        val pet: Pet,
        val byPlayerId: Int,
    ) : PetEvent

    data class Fed(
        val pet: Pet,
        val points: Int,
    ) : PetEvent

    data class HorseJumped(
        val roomOwnerId: Int,
        val riderId: Int,
        val streak: Int,
    ) : PetEvent

    data class Mounted(
        val mount: Pet?,
        val riderId: Int,
    ) : PetEvent

    data class PlantsInRoom(
        val roomOwnerId: Int,
        val pets: Collection<Pet>,
    ) : PetEvent

    /** Two plants bred: every watching owner who got no seed is unlucky. */
    data class SeedsHandedOut(
        val watchers: Collection<Int>,
        val firstSeedTo: Int,
        val secondSeedTo: Int,
    ) : PetEvent

    data class PetAcquired(
        val ownerId: Int,
        val pet: Pet,
    ) : PetEvent
}
