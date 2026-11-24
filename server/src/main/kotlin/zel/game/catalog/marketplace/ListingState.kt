package zel.game.catalog.marketplace

@Suppress("MagicNumber") // the numbers the database and the client use
enum class ListingState(
    val code: Int,
) {
    OPEN(1),
    SOLD(2),
    CLOSED(3),
    ;

    companion object {
        fun fromCode(code: Int): ListingState = entries.firstOrNull { it.code == code } ?: CLOSED
    }
}
