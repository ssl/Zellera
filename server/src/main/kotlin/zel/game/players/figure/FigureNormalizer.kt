package zel.game.players.figure

object FigureNormalizer {
    fun normalize(
        data: Figuredata,
        look: String,
    ): String {
        if (look.isEmpty() || data.setTypes.isEmpty()) return look
        val parts = look.split(".").filter { it.isNotEmpty() }.map { it.split("-") }
        val order = data.typeOrder
        val known = order.flatMap { type -> parts.filter { it[0] == type }.map { normalizePart(data, it) } }
        val unknown = parts.filter { it[0] !in order }.map { it.joinToString("-") }
        return (known + unknown).joinToString(".")
    }

    private fun normalizePart(
        data: Figuredata,
        part: List<String>,
    ): String {
        val raw = part.joinToString("-")
        val settype = data.setTypes[part[0]]
        if (settype == null || part.size < 2) return raw
        val setId = part[1].toIntOrNull() ?: return raw
        val set = settype.set(setId) ?: return raw
        val colors = if (set.colorable) colorsOf(data, settype, set, part) else emptyList()
        return (listOf(part[0], part[1]) + colors).joinToString("-")
    }

    private fun colorsOf(
        data: Figuredata,
        settype: Figuredata.SetType,
        set: Figuredata.ClothingSet,
        part: List<String>,
    ): List<String> {
        val first =
            data.palettes[settype.paletteId]
                ?.defaultColor
                ?.id
                ?.toString()
        return (0 until set.colorCount).mapNotNull { i -> part.getOrNull(2 + i)?.takeIf(String::isNotEmpty) ?: first }
    }
}
