package zel.game.catalog

import zel.game.items.Item
import java.util.Random
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.IntFunction
import java.util.function.IntUnaryOperator

object RecyclerPrizes {
    const val LEVELS = 6

    const val CHAT_STYLE = "chat_style"

    data class Row(
        val level: Int,
        val itemId: Int,
        val itemName: String?,
        val chatStyleId: Int,
        val order: Int,
    )

    data class Entry(
        val level: Int,
        val productCode: String,
        val productType: String,
        val typeId: Int,
        val item: Item?,
    ) {
        /** The bubble a chat style prize gives; 0 for furni. */
        val chatStyleId: Int get() = if (item == null) typeId else 0

        companion object {
            fun furni(
                level: Int,
                item: Item,
            ): Entry = Entry(level, item.name, item.type.code.lowercase(), item.spriteId, item)

            fun chatStyle(
                level: Int,
                styleId: Int,
            ): Entry = Entry(level, "chat_bubble_$styleId", CHAT_STYLE, styleId, null)
        }
    }

    fun resolve(
        rows: List<Row>,
        byId: IntFunction<Item?>,
        byName: Function<String, Item?>,
        missing: Consumer<Row>,
    ): List<Entry> =
        rows
            .sortedWith(
                compareByDescending<Row> { it.level }
                    .thenBy { it.order }
                    .thenBy { it.itemId }
                    .thenBy { it.chatStyleId },
            ).mapNotNull { row ->
                val entry = entryOf(row, byId, byName)
                if (entry == null) missing.accept(row)
                entry
            }

    private fun entryOf(
        row: Row,
        byId: IntFunction<Item?>,
        byName: Function<String, Item?>,
    ): Entry? =
        when {
            row.level < 1 -> {
                null
            }

            row.chatStyleId > 0 -> {
                Entry.chatStyle(row.level, row.chatStyleId)
            }

            else -> {
                val item =
                    (if (row.itemId > 0) byId.apply(row.itemId) else null)
                        ?: row.itemName?.takeUnless { it.isBlank() }?.let { byName.apply(it) }
                item?.let { Entry.furni(row.level, it) }
            }
        }

    fun byLevel(entries: List<Entry>): Map<Int, List<Entry>> =
        entries
            .sortedByDescending { it.level }
            .groupBy { it.level }

    /**
     * One draw decides the level: from the rarest down each level takes its 1 in `odds(n)` share of the draw and
     * level 1 what is left, so the odds the prize list shows are the odds played.
     */
    fun drawLevel(
        odds: IntUnaryOperator,
        random: Random,
    ): Int {
        var draw = random.nextDouble()
        for (level in LEVELS downTo 2) {
            val oneIn = odds.applyAsInt(level)
            if (oneIn <= 0) continue
            val share = 1.0 / oneIn
            if (draw < share) return level
            draw -= share
        }
        return 1
    }
}
