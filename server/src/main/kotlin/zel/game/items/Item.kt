package zel.game.items

import java.sql.ResultSet

/** A reload swaps the row in place: written by the loader thread, read by every other. */
class Item(
    set: ResultSet,
    private val interactionFor: (String) -> ItemInteraction,
    private val texts: () -> FurniTitles?,
    val services: ItemServices,
) {
    @Volatile
    private var row = rowOf(set)

    val id: Int get() = row.id
    val spriteId: Int get() = row.spriteId

    val name: String get() = row.code

    val displayName: String get() = texts()?.titleOf(name) ?: row.displayName

    val type: ProductKind get() = row.kind
    val interaction: ItemInteraction get() = row.interaction
    val width: Int get() = row.width
    val length: Int get() = row.length
    val height: Double get() = row.stackHeight
    val stateCount: Int get() = row.stateCount
    val stateHeights: DoubleArray get() = row.stateHeights

    /** This type's usage policy, null when it has no fixed one. */
    val usagePolicy: Int? get() = row.usagePolicy
    val effectMale: Int get() = row.effectMale
    val effectFemale: Int get() = row.effectFemale
    val canStack: Boolean get() = row.canStack
    val canWalk: Boolean get() = row.canWalk
    val canSit: Boolean get() = row.canSit
    val canLay: Boolean get() = row.canLay
    val canRecycle: Boolean get() = row.canRecycle
    val canTrade: Boolean get() = row.canTrade
    val canSell: Boolean get() = row.canSell
    val canGift: Boolean get() = row.canGift
    val stacksInInventory: Boolean get() = row.stacksInInventory

    val params: String get() = row.behaviourParams

    val isPet: Boolean get() = PetProducts.has(name)
    val isBot: Boolean get() = type == ProductKind.BOT
    val isPermanentEffect: Boolean get() = isPermanentEffect(type, params)

    fun allowsDirection(direction: Int): Boolean = row.directions.allows(direction)

    fun hasDiagonalDirections(): Boolean = row.directions.hasDiagonals

    fun shownDirection(direction: Int): Int = row.directions.shown(direction)

    /** The directions the art is drawn in, lowest first; empty when not known. */
    val drawnDirections: List<Int> get() = row.directions.drawn

    fun directionAfterMove(
        asked: Int,
        old: Int,
    ): Int = if (allowsDirection(asked)) asked else old

    fun randomHandItem(): Int {
        val items = row.handItems
        return if (items.isEmpty()) 0 else items[services.random().nextInt(items.size)]
    }

    fun reload(set: ResultSet) {
        row = rowOf(set)
    }

    private fun rowOf(set: ResultSet): FurniDefinitionRow =
        FurniDefinitionRow.read(set, interactionFor).let { if (it.isWebApiBox) it.withWebApiRules() else it }

    override fun toString(): String = "Item($id $name)"

    companion object {
        private const val PERMANENT = "permanent"

        fun isPermanentEffect(
            type: ProductKind,
            params: String?,
        ): Boolean = type == ProductKind.AVATAR_EFFECT && params?.trim().equals(PERMANENT, ignoreCase = true)
    }
}
