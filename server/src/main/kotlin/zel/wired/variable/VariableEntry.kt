package zel.wired.variable

data class VariableEntry(
    val id: String,
    val type: Int,
    val name: String,
    val availability: Int,
    val target: Int,
    val flags: VariableFlags,
    val texts: ValueTexts? = null,
) {
    val hash: Int get() = hashCode()

    fun textOf(value: Long): String =
        texts?.entries?.firstOrNull { it.first.toLong() == value }?.second ?: value.toString()

    companion object {
        private const val HASH_STEP = 31

        /** The hash of a whole variable list, the one the client compares its cache against. */
        fun listHash(entries: Iterable<VariableEntry>): Int = entries.fold(1) { sum, one -> HASH_STEP * sum + one.hash }
    }
}
