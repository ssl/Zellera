package zel.game.catalog

import zel.game.catalog.layouts.PageForm

class CatalogSnapshot private constructor(
    val version: Long,
    private val offers: Map<CatalogMode, Map<Int, CatalogOffer>>,
    private val pagesByCode: Map<String, CatalogPage>,
    private val pagesByRole: Map<PageForm.Role, CatalogPage>,
    private val clubGifts: Map<Int, CatalogOffer>,
    private val clothing: Map<String, ClothingSet>,
) {
    fun offer(
        mode: CatalogMode,
        id: Int,
    ): CatalogOffer? = offers[mode]?.get(id)

    /** Any case; an open page beats a folder, a folder a hidden page, then the lower id. */
    fun page(code: String?): CatalogPage? = code?.let { pagesByCode[it.lowercase()] }

    fun pageWithRole(role: PageForm.Role): CatalogPage? = pagesByRole[role]

    fun clubGift(id: Int): CatalogOffer? = clubGifts[id]

    fun clothing(name: String?): ClothingSet? = name?.let { clothing[it.lowercase()] }

    companion object {
        fun of(
            contents: CatalogData,
            version: Long,
        ): CatalogSnapshot {
            val trees =
                listOf(CatalogMode.NORMAL, CatalogMode.BUILDER).associateWith { mode ->
                    contents.tree(mode).let { live -> synchronized(live) { live.values.toList() } }.sortedBy { it.id }
                }
            val normal = trees.getValue(CatalogMode.NORMAL)
            return CatalogSnapshot(
                version = version,
                offers = trees.mapValues { (_, pages) -> pages.flatMap(::offersOf).associateBy { it.id } },
                pagesByCode =
                    normal
                        .filter { it.info.code.isNotEmpty() }
                        .groupingBy { it.info.code.lowercase() }
                        .reduce { _, best, next -> betterNamed(best, next) },
                pagesByRole =
                    normal
                        .filter {
                            it.isOpen && it.minRank <= 0 && !it.info.clubOnly &&
                                it.form.role != PageForm.Role.NONE
                        }.groupingBy { it.form.role }
                        .reduce { _, best, next -> if (next.id < best.id) next else best },
                clubGifts =
                    contents.clubGifts.let { live -> synchronized(live) { live.toList() } }.associateBy { it.id },
                clothing = contents.clothingSets.values.associateBy { it.name.orEmpty().lowercase() },
            )
        }

        fun betterNamed(
            a: CatalogPage,
            b: CatalogPage,
        ): CatalogPage = if (compareValuesBy(b, a, ::nameWeight, { -it.id }) > 0) b else a

        private fun nameWeight(page: CatalogPage): Int =
            when {
                page.isOpen -> 2
                page.info.visible -> 1
                else -> 0
            }

        private fun offersOf(page: CatalogPage): List<CatalogOffer> =
            synchronized(page.offers) { page.offers.values.toList() }
    }
}
