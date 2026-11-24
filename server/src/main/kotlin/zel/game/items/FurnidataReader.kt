package zel.game.items

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class FurnidataReader(
    private val location: Path,
    private val sizeCap: Long,
) {
    private class OverCap(
        file: Path,
        cap: Long,
    ) : IOException("$file is over $cap bytes")

    fun read(): List<FurnidataEntry> =
        runCatching { files().flatMap(::typesOrSkip) }
            .onFailure { LOGGER.warn("furnidata at {} not read: {}", location, it.toString()) }
            .getOrDefault(emptyList())

    private fun typesOrSkip(file: Path): List<FurnidataEntry> =
        try {
            typesIn(file)
        } catch (tooBig: OverCap) {
            throw tooBig
        } catch (unreadable: IOException) {
            skipped(file, unreadable.toString())
        } catch (notJson: JsonParseException) {
            skipped(file, notJson.toString())
        }

    private fun skipped(
        file: Path,
        why: String,
    ): List<FurnidataEntry> {
        LOGGER.warn("furnidata fragment {} skipped: {}", file, why)
        return emptyList()
    }

    private fun files(): List<Path> =
        when {
            Files.isDirectory(location) -> fragments()
            Files.isRegularFile(location) && Jsonc.isSupported(location) -> listOf(location)
            else -> emptyList()
        }

    private fun fragments(): List<Path> {
        val top = location.normalize()
        val tierFolders = (listedIn(location, "tiers") ?: listOf(DEFAULT_TIER)).map(location::resolve)
        return tierFolders
            .flatMap { folder -> listedIn(folder, "files").orEmpty().map { folder.resolve(it).normalize() } }
            .filter { isReadable(it, top) }
    }

    private fun isReadable(
        fragment: Path,
        top: Path,
    ): Boolean {
        val why =
            when {
                !fragment.startsWith(top) -> "it leads out of $top"
                !Jsonc.isSupported(fragment) -> "not a .json or .jsonc file"
                else -> return true
            }
        skipped(fragment, why)
        return false
    }

    private fun listedIn(
        folder: Path,
        key: String,
    ): List<String>? =
        MANIFESTS
            .map(folder::resolve)
            .firstOrNull(Files::isRegularFile)
            ?.let { readJson(it)[key] as? JsonArray }
            ?.filter { it.isJsonPrimitive }
            ?.map { it.asString }

    private fun readJson(file: Path): JsonObject {
        if (Files.size(file) > sizeCap) throw OverCap(file, sizeCap)
        return Jsonc.parse(Files.readString(file))
    }

    private fun typesIn(file: Path): List<FurnidataEntry> {
        val parsed = readJson(file)
        return KINDS.flatMap { (key, kind) ->
            ((parsed[key] as? JsonObject)?.get("furnitype") as? JsonArray ?: JsonArray())
                .mapNotNull { (it as? JsonObject)?.let { type -> entryOf(type, kind) } }
        }
    }

    private fun entryOf(
        type: JsonObject,
        kind: ProductKind,
    ): FurnidataEntry? {
        val id = type.primitive("id")?.asInt ?: return null
        val classname = type.primitive("classname")?.asString ?: return null
        return FurnidataEntry(id, classname, kind, type.text("name"), type.text("description"))
    }

    private fun JsonObject.primitive(key: String) = get(key)?.takeIf { it.isJsonPrimitive }

    private fun JsonObject.text(key: String): String = primitive(key)?.asString.orEmpty()

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(FurnidataReader::class.java)
        const val DEFAULT_TIER = "core"
        val MANIFESTS = listOf("manifest.json", "manifest.jsonc")
        val KINDS = listOf("roomitemtypes" to ProductKind.FLOOR_FURNI, "wallitemtypes" to ProductKind.WALL_FURNI)
    }
}
