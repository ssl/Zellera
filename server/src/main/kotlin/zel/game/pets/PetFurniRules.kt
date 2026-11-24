package zel.game.pets

import zel.game.items.Item

/** Which pet types a pet furni (food, drink, nest, toy) is for; a furni without a row is for every type. */
class PetFurniRules {
    private val typesByFurni = HashMap<Int, MutableSet<Int>>()

    fun keepFor(
        furniId: Int,
        type: Int,
    ) {
        typesByFurni.getOrPut(furniId) { HashSet() }.add(type)
    }

    fun allows(
        furni: Item,
        type: Int,
    ): Boolean = typesByFurni[furni.id]?.contains(type) ?: true
}
