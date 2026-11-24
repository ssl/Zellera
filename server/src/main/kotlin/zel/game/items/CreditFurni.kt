package zel.game.items

object CreditFurni {
    private val VALUE = Regex("^CFC?_(?:diamond_)?(\\d+)(?:_|$)")

    fun credits(classname: String?): Int =
        VALUE
            .find(classname.orEmpty())
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull() ?: 0

    fun credits(baseItem: Item?): Int = baseItem?.name?.let(::credits) ?: 0
}
