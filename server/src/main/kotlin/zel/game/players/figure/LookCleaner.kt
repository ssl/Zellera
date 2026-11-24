package zel.game.players.figure

class LookCleaner(
    private val data: Figuredata,
) {
    fun clean(
        look: String,
        wearer: Figuredata.Wearer,
    ): String {
        if (data.isEmpty) return look
        val given =
            look
                .split('.')
                .filter { it.contains('-') }
                .map { it.split('-') }
                .associateBy { it[0] }
        return data.setTypes.values
            .mapNotNull { setType ->
                val part = given[setType.type]
                when {
                    part != null -> worn(setType, part, wearer)
                    setType.optionalFor(wearer) -> null
                    else -> fallback(setType, wearer)
                }
            }.joinToString(".")
    }

    /** A malformed part goes; an unknown set stays as given. */
    private fun worn(
        setType: Figuredata.SetType,
        part: List<String>,
        wearer: Figuredata.Wearer,
    ): String? {
        val setId = part[1].toIntOrNull() ?: return null
        val set = setType.set(setId) ?: return part.joinToString("-")
        if (!set.wearableBy(wearer)) return if (setType.optionalFor(wearer)) null else fallback(setType, wearer)
        val colors =
            part.drop(2).take(colorSlots(set)).map { given ->
                allowedColor(setType, given.toIntOrNull() ?: return null, wearer) ?: return null
            }
        return write(setType.type, set.id, colors)
    }

    private fun fallback(
        setType: Figuredata.SetType,
        wearer: Figuredata.Wearer,
    ): String? {
        val set = setType.fallbackFor(wearer.gender) ?: return null
        val free = data.palettes[setType.paletteId]?.freeColor?.id
        return write(setType.type, set.id, List(colorSlots(set)) { free ?: return null })
    }

    private fun allowedColor(
        setType: Figuredata.SetType,
        given: Int,
        wearer: Figuredata.Wearer,
    ): Int? {
        val palette = data.palettes[setType.paletteId] ?: return null
        val color = palette.color(given)
        val usable = color != null && color.selectable && (!color.clubOnly || wearer.club)
        return if (usable) color.id else palette.freeColor?.id
    }

    private fun colorSlots(set: Figuredata.ClothingSet): Int = if (set.colorable) maxOf(1, set.colorCount) else 0

    private fun write(
        type: String,
        setId: Int,
        colors: List<Int>,
    ): String = (listOf(type, setId.toString()) + colors.map(Int::toString)).joinToString("-")
}
