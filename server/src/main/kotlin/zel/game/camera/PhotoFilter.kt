package zel.game.camera

sealed interface PhotoFilter {
    /** 4 rows of 5 values, offsets in 0..255. */
    class Matrix(
        val values: DoubleArray,
    ) : PhotoFilter

    data class Overlay(
        val blend: BlendMode,
    ) : PhotoFilter

    data object Border : PhotoFilter
}
