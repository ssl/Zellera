package zel.room.furni

import zel.room.furni.place.Interactions

/**
 * Furni whose state holds a value of their own (hits, an engraving, a rent, contents) instead of a look: saved data of
 * the furni. A wired room rollback and wired state writes leave it as it is.
 */
object StoredValues {
    fun holds(furni: RoomFurni): Boolean = holds(Interactions.of(furni.definition))

    fun holds(type: String): Boolean = type.startsWith(CRACKABLE) || type in TYPES

    /** Every crackable (plain, master, monster, subscription): its state is the hit count. */
    private const val CRACKABLE = "crack."

    private val TYPES =
        setOf(
            // signer, day and engraved text
            "lock.love",
            "display.trophy",
            // badge code, signer and day
            "display.badge",
            // renter and paid-until
            "area.rental",
            // saved outfit and its name
            "outfit.mannequin",
            // the saved male and female outfits
            "game.football.gate",
            // wrapping and contents
            "gift.box",
            // colour and text of the note
            Interactions.POST_IT,
            // the photo
            "display.image",
            // rarity of the seed
            "pet.seed",
            // growth frame, in step with the saved growth
            "plant.pot",
        )
}
