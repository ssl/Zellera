package zel.game.items

import zel.core.Configuration
import java.nio.file.Files
import java.nio.file.Path

sealed interface FurnidataLocation {
    data class Found(
        val path: Path,
        val label: String,
    ) : FurnidataLocation

    data class Absent(
        val reason: String,
    ) : FurnidataLocation

    private enum class Shape {
        FOLDER {
            override fun fits(at: Path) = Files.isDirectory(at)
        },
        FILE {
            override fun fits(at: Path) = Files.isRegularFile(at) && Jsonc.isSupported(at)
        },
        ;

        abstract fun fits(at: Path): Boolean
    }

    private class Candidate(
        val path: Path,
        val shape: Shape,
        val label: String,
    )

    companion object {
        const val OVERRIDE_KEY = "furni.data.location"
        const val ASSET_BASE_KEY = "furni.data.assets.folder"
        private const val SPLIT_FOLDER = "furnidata"
        private const val SINGLE_FILE = "FurnitureData.json"

        fun configured(config: Configuration): FurnidataLocation =
            pick(config.getValue(OVERRIDE_KEY, ""), config.getValue(ASSET_BASE_KEY, ""))

        /** The first place that is there: the asset base's split folder, its single file, then the override. */
        fun pick(
            overrideValue: String,
            assetBaseValue: String,
        ): FurnidataLocation {
            val assetBase = named(assetBaseValue)
            val override = named(overrideValue)
            val search =
                buildList {
                    if (assetBase != null) {
                        val folder = assetBase.resolve(SPLIT_FOLDER)
                        add(Candidate(folder, Shape.FOLDER, "split furnidata folder of the asset base"))
                        add(Candidate(assetBase.resolve(SINGLE_FILE), Shape.FILE, "furnidata file of the asset base"))
                    }
                    if (override != null) {
                        add(Candidate(override, Shape.FOLDER, "split furnidata folder $override"))
                        add(Candidate(override, Shape.FILE, "furnidata file $override"))
                    }
                }
            val found = search.firstOrNull { it.shape.fits(it.path) }
            return when {
                found != null -> Found(found.path, found.label)
                override != null -> Absent("$override is no .json/.jsonc file or folder")
                assetBase != null -> Absent("no $SPLIT_FOLDER/ or $SINGLE_FILE under $assetBase")
                else -> Absent("no furnidata source configured")
            }
        }

        private fun named(value: String): Path? = value.trim().ifEmpty { null }?.let(Path::of)
    }
}
