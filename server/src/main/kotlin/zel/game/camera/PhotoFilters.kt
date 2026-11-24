package zel.game.camera

/** The filters a frame may choose, by name, as the content defines them. */
internal object PhotoFilters {
    operator fun get(name: String): PhotoFilter? = CameraAssetData.filter(name)

    /** A matrix as `r | g | b [| a]` rows of five numbers each; a missing alpha row leaves alpha as it is. */
    fun matrix(text: String): PhotoFilter.Matrix =
        PhotoFilter.Matrix(
            text
                .split('|')
                .map(String::trim)
                .let { rows -> rows + if (rows.size == ROWS_WITHOUT_ALPHA) listOf(PLAIN_ALPHA_ROW) else emptyList() }
                .flatMap { it.split(' ') }
                .map(String::toDouble)
                .toDoubleArray(),
        )

    private const val PLAIN_ALPHA_ROW = "0 0 0 1 0"
    private const val ROWS_WITHOUT_ALPHA = 3
}
