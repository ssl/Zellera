package zel.wired.variable

fun interface ValueTables {
    fun of(id: String): ValueTexts?

    companion object {
        val None: ValueTables = ValueTables { null }
    }
}
