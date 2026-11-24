package zel.protocol.outgoing.inventory

import zel.game.items.OwnedFurni
import zel.protocol.generated.UnseenItemsOut
import zel.protocol.generated.UnseenItemsOutCodec
import zel.protocol.outgoing.OutgoingMessage

class AddRoomItem private constructor(
    private val groups: Map<Int, List<Int>>,
) : OutgoingMessage<UnseenItemsOut>(UnseenItemsOutCodec) {
    constructor(furni: OwnedFurni) : this(listOf(furni))

    /** Floor furni strip ids are negative, wall furni positive. */
    constructor(furni: Collection<OwnedFurni>) : this(group(UnseenCategory.OWNED_FURNI, furni.map { it.stripId }))

    constructor(id: Int, category: UnseenCategory) : this(group(category, listOf(id)))

    constructor(ids: IntArray, category: UnseenCategory) : this(group(category, ids.asList()))

    fun unseenByCategory(): Map<Int, List<Int>> = groups

    override fun message() =
        UnseenItemsOut(groups.map { (category, ids) -> UnseenItemsOut.CategoriesItem(category, ids) })

    /** The client's inventory tabs; it has no tab for effects, so a bought effect goes out under [ICON]. */
    @Suppress("MagicNumber") // the tab numbers the client reads
    enum class UnseenCategory(
        val number: Int,
    ) {
        OWNED_FURNI(1),
        PET(3),
        BADGE(4),
        BOT(5),
        GAME(6),
        COLLECTIBLE(7),
        ICON(8),
        EARNINGS(10),
    }

    companion object {
        private fun group(
            category: UnseenCategory,
            ids: List<Int>,
        ): Map<Int, List<Int>> = mapOf(category.number to ids)

        fun byCategoryNumber(byNumber: Map<Int, List<Int>>): AddRoomItem =
            AddRoomItem(byNumber.entries.associateTo(LinkedHashMap()) { it.key to it.value.toList() })
    }
}
