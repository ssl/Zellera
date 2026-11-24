package zel.game.pets

data class PetPackage(
    val itemName: String,
    val petType: Int,
    val palette: Int,
    val color: String,
    val nestItemName: String?,
) {
    fun figure(): String = "$petType $palette $color"
}
