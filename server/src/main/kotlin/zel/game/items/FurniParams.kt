package zel.game.items

/**
 * A definition's params as the furni data writes them: values split by commas, groups of values by ':' or ';'.
 * A behaviour reads the value it knows the meaning of and keeps its own number where the furni names none.
 */
object FurniParams {
    fun int(
        params: String,
        index: Int,
    ): Int? = values(params).getOrNull(index)?.toIntOrNull()

    fun ints(params: String): List<Int> = values(params).mapNotNull(String::toIntOrNull)

    fun values(params: String): List<String> = params.split(',').map(String::trim)

    fun groups(params: String): List<String> = params.split(':', ';').map(String::trim)
}
