package zel.room.furni.behaviour.special

import zel.game.items.ItemDataUrls
import zel.protocol.generated.StuffData
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.state.CustomValues
import zel.room.furni.behaviour.state.StateShow
import zel.room.furni.place.StuffOnWire

/** The image must be a path on the asset mirror, else none; a foreign link goes out as the site's own page. */
class RoomAds(
    private val site: () -> String,
) : ObjectDataEditable,
    StuffOnWire {
    override fun stuff(furni: RoomFurni): StuffData =
        SpecialStuff.of(
            furni,
            SpecialStuff.MAP,
            StuffData.Value.MapData(values(furni.state).map { (key, value) -> key to clientValue(key, value) }),
        )

    override fun edit(
        scope: RoomScope,
        furni: RoomFurni,
        values: Map<String, String>,
    ) {
        val current = values(furni.state)
        values.filter { (key, value) -> key in DEFAULTS && fits(value) }.forEach { (key, value) ->
            if (key != IMAGE) {
                current[key] = value
            } else if (value.isBlank()) {
                current[key] = ""
            } else {
                ItemDataUrls.adImage(value, site())?.let { current[key] = it }
            }
        }
        val stored = CustomValues.write(current)
        if (stored.length <= CustomValues.MAX_STORED) StateShow.whole(scope, furni, stored)
    }

    /** Pairs end at `;` and split at their first `=`, so only a `;` would write keys of its own. */
    private fun fits(value: String): Boolean = value.length <= MAX_VALUE && ';' !in value

    private fun clientValue(
        key: String,
        value: String,
    ): String =
        when (key) {
            IMAGE -> ItemDataUrls.adImage(value, site()).orEmpty()
            CLICK -> ItemDataUrls.link(value, site()) ?: value
            else -> value
        }

    companion object {
        private const val IMAGE = "imageUrl"
        private const val CLICK = "clickUrl"
        private const val MAX_VALUE = 512

        private val DEFAULTS =
            mapOf(
                IMAGE to "",
                CLICK to "",
                "offsetX" to "0",
                "offsetY" to "0",
                "offsetZ" to "0",
                "scale" to "100",
            )

        fun values(state: String): HashMap<String, String> = HashMap(CustomValues.read(state, DEFAULTS))
    }
}
