package zel.wired.effect.user

import zel.protocol.generated.WiredFurniVisualizationStateOut
import zel.protocol.generated.WiredPlayerVisualizationStateOut

class KeptVisuals {
    private val furniTransparency = LinkedHashMap<Int, Transparency>()
    private val furniHighlight = LinkedHashMap<Int, Highlight>()
    private val userTransparency = LinkedHashMap<Int, Transparency>()
    private val userHighlight = LinkedHashMap<Int, Highlight>()

    fun furni(
        ids: List<Int>,
        value: Transparency,
    ) = ids.forEach { furniTransparency[it] = value }

    fun furni(
        ids: List<Int>,
        value: Highlight,
    ) = ids.forEach { furniHighlight[it] = value }

    fun users(
        indexes: List<Int>,
        value: Transparency,
    ) = indexes.forEach { userTransparency[it] = value }

    fun users(
        indexes: List<Int>,
        value: Highlight,
    ) = indexes.forEach { userHighlight[it] = value }

    fun userLeft(index: Int) {
        userTransparency.remove(index)
        userHighlight.remove(index)
    }

    fun furniGone(id: Int) {
        furniTransparency.remove(id)
        furniHighlight.remove(id)
    }

    fun keepFurni(present: (Int) -> Boolean) {
        furniTransparency.keys.retainAll(present)
        furniHighlight.keys.retainAll(present)
    }

    fun furniState(): WiredFurniVisualizationStateOut? =
        if (furniTransparency.isEmpty() && furniHighlight.isEmpty()) {
            null
        } else {
            WiredFurniVisualizationStateOut(
                furniTransparency.map { (id, v) ->
                    WiredFurniVisualizationStateOut.List1Item(id, v.level, v.clickThrough, v.easing)
                },
                furniHighlight.map { (id, v) ->
                    WiredFurniVisualizationStateOut.List2Item(id, v.type, v.strength, v.color, v.animation, v.easing)
                },
            )
        }

    fun userState(): WiredPlayerVisualizationStateOut? =
        if (userTransparency.isEmpty() && userHighlight.isEmpty()) {
            null
        } else {
            WiredPlayerVisualizationStateOut(
                userTransparency.map { (id, v) ->
                    WiredPlayerVisualizationStateOut.List1Item(id, v.level, v.clickThrough, v.easing)
                },
                userHighlight.map { (id, v) ->
                    WiredPlayerVisualizationStateOut.List2Item(id, v.type, v.strength, v.color, v.animation, v.easing)
                },
            )
        }
}
