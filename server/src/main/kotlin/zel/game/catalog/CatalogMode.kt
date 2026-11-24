package zel.game.catalog

enum class CatalogMode(
    val code: String,
) {
    NORMAL("NORMAL"),
    BUILDER("BUILD_PASS"),
    BOTH("BOTH"),
    ;

    fun lists(pageMode: CatalogMode): Boolean = pageMode == this || pageMode == BOTH

    companion object {
        fun fromCode(code: String?): CatalogMode = if (code == BUILDER.code) BUILDER else NORMAL
    }
}
