package zel.game.pets

class PetsModule(
    val services: PetServices,
    val breedingCurve: BreedingCurve = BreedingCurve(),
) {
    val catalog = PetCatalog(services)
    val maker = PetMaker(this)

    fun acquired(
        ownerId: Int,
        pet: Pet,
    ) = PetAchievements.credit(pet.services, PetEvent.PetAcquired(ownerId, pet))
}
