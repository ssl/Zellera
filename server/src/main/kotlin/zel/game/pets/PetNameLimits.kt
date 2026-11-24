package zel.game.pets

/**
 * The pet name lengths the catalog and pet furni accept; set from the game settings.
 * Its settings are @JvmField: an object property's setter would write a static field (SpotBugs ST).
 */
object PetNameLimits {
    @JvmField
    @Volatile
    var minimum = 3

    @JvmField
    @Volatile
    var maximum = 15
}
