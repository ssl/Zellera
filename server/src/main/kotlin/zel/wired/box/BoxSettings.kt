package zel.wired.box

data class BoxSettings(
    val ints: List<Int>,
    val text: String,
    val picks: List<Int>,
    val picks2: List<Int>,
    val furniSources: List<Int>,
    val userSources: List<Int>,
    val quantifier: Int,
    val delay: Int,
    val variableIds: List<String>,
    val filter: Boolean,
    val invert: Boolean,
    val furniStates: Map<Int, String> = emptyMap(),
    val snapshots: List<FurniSnapshot> = emptyList(),
) {
    /** The int at [index], or [fresh] when the form is shorter. */
    fun intAt(
        index: Int,
        fresh: Int = 0,
    ): Int = ints.getOrElse(index) { fresh }

    companion object {
        val EMPTY =
            BoxSettings(
                ints = emptyList(),
                text = "",
                picks = emptyList(),
                picks2 = emptyList(),
                furniSources = emptyList(),
                userSources = emptyList(),
                quantifier = 0,
                delay = 0,
                variableIds = emptyList(),
                filter = false,
                invert = false,
            )
    }
}
