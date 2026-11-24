package zel.game.pets

data class PetPalette(
    val petType: Int,
    val breedId: Int,
    val paletteId: Int,
    val sellable: Boolean,
    val rare: Boolean,
    val membersOnly: Boolean,
)
