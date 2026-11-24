package zel.game.items

import java.io.IOException
import java.io.UncheckedIOException
import java.util.Properties

class RecolorablePalettes private constructor(
    private val palettes: Map<String, List<Int>>,
    private val paletteOfFurni: Map<String, String>,
) {
    fun colors(itemName: String): List<Int> =
        palettes[paletteOfFurni.getOrDefault(itemName, DEFAULT_PALETTE)]
            ?: palettes[DEFAULT_PALETTE].orEmpty()

    fun defaultColor(itemName: String): Int = colors(itemName).firstOrNull() ?: WHITE

    fun allows(
        itemName: String,
        rgb: Int,
    ): Boolean = rgb in colors(itemName)

    companion object {
        const val DEFAULT_PALETTE = "default"
        private const val RESOURCE = "/recolorable/palettes.properties"
        private const val PALETTE_PREFIX = "recolorable_bc.palette."
        private const val PALETTE_TO_FURNI = "recolorable_bc.palette_to_furni"
        private const val WHITE = 0xFFFFFF
        private const val HEX = 16

        private val instance: RecolorablePalettes = load()

        fun get(): RecolorablePalettes = instance

        private fun parse(properties: Properties): RecolorablePalettes {
            val palettes =
                properties
                    .stringPropertyNames()
                    .filter { it.startsWith(PALETTE_PREFIX) }
                    .associate { key ->
                        key.removePrefix(PALETTE_PREFIX) to
                            properties.getProperty(key).split(",").map { it.trim().toInt(HEX) }
                    }
            val paletteOfFurni = HashMap<String, String>()
            for (entry in properties.getProperty(PALETTE_TO_FURNI, "").split(";")) {
                val separator = entry.indexOf('=')
                if (separator <= 0) continue
                val palette = entry.substring(0, separator).trim()
                entry
                    .substring(separator + 1)
                    .split(",")
                    .filter { it.isNotBlank() }
                    .forEach { paletteOfFurni[it.trim()] = palette }
            }
            return RecolorablePalettes(palettes, paletteOfFurni)
        }

        private fun load(): RecolorablePalettes {
            val properties = Properties()
            try {
                RecolorablePalettes::class.java.getResourceAsStream(RESOURCE)?.use(properties::load)
            } catch (exception: IOException) {
                throw UncheckedIOException(exception)
            }
            return parse(properties)
        }
    }
}
