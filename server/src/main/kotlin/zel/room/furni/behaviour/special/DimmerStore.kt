package zel.room.furni.behaviour.special

interface DimmerStore {
    data class StoredPreset(
        val selected: Boolean,
        val preset: Dimmers.Preset,
    )

    fun load(roomId: Int): Dimmers.Presets?

    /** Runs on the persistence executor. */
    fun save(
        roomId: Int,
        presets: Dimmers.Presets,
    )

    companion object {
        val None: DimmerStore =
            object : DimmerStore {
                override fun load(roomId: Int): Dimmers.Presets? = null

                override fun save(
                    roomId: Int,
                    presets: Dimmers.Presets,
                ) = Unit
            }

        private const val NEVER_SAVED_COLOUR = "#000000"
        private const val NEVER_SAVED_BRIGHTNESS = 255

        /** What a room that never saved a preset has; its two last presets carry the same id. */
        val NEVER_SAVED =
            Dimmers.Presets(
                listOf(1, 3, 3).map { Dimmers.Preset(it, false, NEVER_SAVED_COLOUR, NEVER_SAVED_BRIGHTNESS) },
                1,
            )

        fun presetsOf(rows: List<StoredPreset>): Dimmers.Presets {
            if (rows.isEmpty()) return NEVER_SAVED
            val chosen = rows.filter { it.selected }
            val selected = chosen.singleOrNull()?.preset?.id ?: rows.first().preset.id
            return Dimmers.Presets(rows.map { it.preset }, selected)
        }
    }
}
