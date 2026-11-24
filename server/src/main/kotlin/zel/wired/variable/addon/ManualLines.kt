package zel.wired.variable.addon

class ManualLines(
    /** Sorted by level; the last line of a level wins. */
    val lines: List<Pair<Int, Int>>,
    val refused: Boolean,
) {
    val text: String get() = lines.joinToString("\n") { (level, xp) -> "$level=$xp" }

    companion object {
        private const val MIN_LEVEL = 2
        private const val MIN_XP = 1

        fun parse(text: String): ManualLines {
            val table = java.util.TreeMap<Int, Int>()
            var refused = false
            text.split('\n').forEach { line ->
                val parts = line.split('=')
                val level = parts.getOrNull(0)?.toIntOrNull()
                val xp = parts.getOrNull(1)?.toIntOrNull()
                if (parts.size == 2 && level != null && xp != null) {
                    if (level < MIN_LEVEL || xp < MIN_XP) refused = true
                    table[level] = xp
                }
            }
            return ManualLines(table.map { it.key to it.value }, refused)
        }
    }
}
