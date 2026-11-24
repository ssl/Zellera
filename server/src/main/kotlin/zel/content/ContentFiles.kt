package zel.content

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption

internal class ContentFiles(
    private val dirs: ContentDirs,
) {
    fun check(pack: ContentPack) {
        entries(pack).forEach { target(it) }
    }

    private fun entries(pack: ContentPack) =
        pack.files.filterNot { it in ContentPack.READ_ENTRIES || it.startsWith(ContentPack.CLIENT_PREFIX) }

    /** Each file goes through a temporary name, so the web never reads half a file. */
    fun write(pack: ContentPack): Int {
        val entries = entries(pack)
        for (entry in entries) {
            val target = target(entry)
            Files.createDirectories(target.parent)
            val part = target.resolveSibling("${target.fileName}.part")
            pack.open(entry).use { Files.copy(it, part, StandardCopyOption.REPLACE_EXISTING) }
            Files.move(part, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }
        return entries.size
    }

    /** The client data files of a pack that are not in the gamedata folder. */
    fun missing(): List<String> {
        val folder = dirs.gamedata ?: return listOf("ZEL_GAMEDATA_DIR is not set")
        return CLIENT_DATA_FILES.values.filterNot { Files.isRegularFile(folder.resolve(it)) }
    }

    private fun target(entry: String): Path =
        when {
            entry.startsWith(CLIENT_DATA) -> {
                clientDataTarget(entry)
            }

            entry.startsWith(BADGE_PARTS) -> {
                dir(dirs.data, "ZEL_DATA_DIR", entry).resolve("badgeparts").resolve(entry.removePrefix(BADGE_PARTS))
            }

            entry.startsWith(ARENAS) -> {
                val name = entry.removePrefix(ARENAS)
                if (!ARENA_FILE.matches(name)) throw ContentPackException("unknown Snowball arena file: $entry")
                dir(dirs.data, "ZEL_DATA_DIR", entry).resolve("snowball/arenas").resolve(name)
            }

            entry.startsWith(ASSETS) -> {
                dir(dirs.assets, "ZEL_ASSET_DIR", entry).resolve(entry.removePrefix(ASSETS))
            }

            else -> {
                throw ContentPackException("no place for $entry")
            }
        }.also(::refuseLinks)

    private fun clientDataTarget(entry: String): Path {
        val name = entry.removePrefix(CLIENT_DATA)
        if (name == OFFERS) return dir(dirs.data, "ZEL_DATA_DIR", entry).resolve(OFFERS_FILE)
        if (name.startsWith(OVERRIDES)) {
            val file = name.removePrefix(OVERRIDES)
            if (file !in OVERRIDE_FILES) throw ContentPackException("unknown override file: $entry")
            return dir(dirs.gamedata, "ZEL_GAMEDATA_DIR", entry).resolve(OVERRIDES.removeSuffix("/")).resolve(file)
        }
        val file = CLIENT_DATA_FILES[name] ?: throw ContentPackException("unknown client data file: $entry")
        return dir(dirs.gamedata, "ZEL_GAMEDATA_DIR", entry).resolve(file)
    }

    private fun dir(
        value: Path?,
        key: String,
        entry: String,
    ): Path = value ?: throw ContentPackException("$key is not set, so $entry has nowhere to go")

    private fun refuseLinks(target: Path) {
        var current = target.parent
        while (current != null && !Files.exists(current, LinkOption.NOFOLLOW_LINKS)) current = current.parent
        if (current != null && Files.isSymbolicLink(current)) {
            throw ContentPackException("refusing to write through the link $current")
        }
    }

    private companion object {
        const val CLIENT_DATA = "clientdata/"
        const val BADGE_PARTS = "badges/parts/"
        const val ASSETS = "assets/"
        const val ARENAS = "snowball/arenas/"
        val ARENA_FILE = Regex("""\d+\.json""")
        const val OFFERS = "furni-offers.json"
        const val OFFERS_FILE = "furnidata-offers.json"

        const val OVERRIDES = "overrides/"
        val OVERRIDE_FILES = setOf("external_texts.txt", "external_variables.txt", "productdata.json")

        val CLIENT_DATA_FILES =
            mapOf(
                "furni.json" to "furnidata",
                "products.json" to "productdata",
                "variables.txt" to "external_variables",
                "texts.txt" to "external_texts",
                "figures.xml" to ContentDirs.FIGUREDATA_FILE,
                "figures.json" to "figurepartlist_json",
                "client-release.txt" to "asset-release.txt",
            )
    }
}
