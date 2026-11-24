package zel.game.camera

import java.util.concurrent.atomic.AtomicReference

/** The camera's filters and base figure libraries, as the content defines them. */
internal object CameraAssetData {
    private class Data(
        val filters: Map<String, PhotoFilter>,
        val base: List<String>,
        val shades: Map<String, Map<Int, Int>>,
    )

    private val data = AtomicReference(Data(emptyMap(), emptyList(), emptyMap()))

    fun install(
        filters: Map<String, PhotoFilter>,
        libraries: List<Library>,
    ) {
        data.set(
            Data(
                filters,
                libraries.filter { it.base }.map { it.name },
                libraries.associate { it.name to it.shades },
            ),
        )
    }

    fun filter(name: String): PhotoFilter? = data.get().filters[name]

    /** Always loaded; parts of the default figure live there unmapped. */
    fun baseLibraries(): List<String> = data.get().base

    /** Grey levels of a figure library drawn lighter, to match the darker skin shading. */
    fun shadesOf(library: String?): Map<Int, Int> = data.get().shades[library].orEmpty()

    class Library(
        val name: String,
        val base: Boolean,
        val shades: Map<Int, Int>,
    )
}
