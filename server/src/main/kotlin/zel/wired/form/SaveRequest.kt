package zel.wired.form

data class SaveRequest(
    val boxId: Int,
    val ints: List<Int>,
    val text: String,
    val picks: List<Int>,
    val picks2: List<Int>,
    val furniSources: List<Int>,
    val userSources: List<Int>,
    val variableIds: List<String>,
    val delay: Int = 0,
    val quantifier: Int = 0,
    val filter: Boolean = false,
    val invert: Boolean = false,
    val saverId: Int = 0,
)
