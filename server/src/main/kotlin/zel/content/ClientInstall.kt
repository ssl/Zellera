package zel.content

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes

/**
 * The built client a pack carries under `client/`. It replaces the release's own files in the client folder; the
 * extension folders and `client.json` entries the pack does not name stay.
 */
internal class ClientInstall(
    private val root: Path?,
) {
    fun names(pack: ContentPack): List<String> =
        pack.files
            .filter { it.startsWith(ContentPack.CLIENT_PREFIX) && it !in ContentPack.READ_ENTRIES }
            .map { it.removePrefix(ContentPack.CLIENT_PREFIX) }

    fun check(pack: ContentPack) {
        val names = names(pack)
        if (names.isEmpty()) return
        names.forEach(::checkName)
        if (CONFIG !in names) throw ContentPackException("the pack's client has no $CONFIG")
        packedConfig(pack)
        folder()
    }

    /** Files go through a temporary name; the stale ones of the release's own folders go; `client.json` goes last. */
    fun write(pack: ContentPack): Int {
        val names = names(pack)
        if (names.isEmpty()) return 0
        val folder = folder()
        val config = mergedConfig(pack, folder)
        for (name in names - CONFIG) {
            replace(
                folder,
                name,
            ) { target -> pack.open(ContentPack.CLIENT_PREFIX + name).use { Files.copy(it, target) } }
        }
        ownedFolders(names).forEach { prune(folder, it, names.toSet()) }
        replace(folder, CONFIG) { Files.writeString(it, GSON.toJson(config) + "\n") }
        return names.size
    }

    private fun folder(): Path =
        root?.normalize()
            ?: throw ContentPackException("ZEL_CLIENT_DIR is not set, so the pack's client has nowhere to go")

    private fun checkName(name: String) {
        val parts = name.split('/')
        if (parts.any { it.isEmpty() || it == "." || it == ".." }) {
            throw ContentPackException("unsafe path in the pack's client: $name")
        }
        if (parts[0] == EXTENSIONS && parts.size < MIN_EXTENSION_PARTS) {
            throw ContentPackException("an extension of the pack's client is a folder: $name")
        }
    }

    private fun replace(
        folder: Path,
        name: String,
        fill: (Path) -> Unit,
    ) {
        val target = folder.resolve(name).normalize()
        if (!target.startsWith(
                folder,
            )
        ) {
            throw ContentPackException("the pack's client would write outside $folder: $name")
        }
        refuseLinks(folder, name)
        Files.createDirectories(target.parent)
        val part = target.resolveSibling("${target.fileName}.part")
        Files.deleteIfExists(part)
        fill(part)
        Files.move(part, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun refuseLinks(
        folder: Path,
        name: String,
    ) {
        var current = folder
        for (part in name.split('/')) {
            current = current.resolve(part)
            if (Files.isSymbolicLink(current)) throw ContentPackException("refusing to write through the link $current")
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) return
        }
    }

    /** The folders the release owns: its top-level ones, and each extension it names. */
    private fun ownedFolders(names: List<String>): Set<String> =
        names
            .map { it.split('/') }
            .filter { it.size > 1 }
            .map { if (it[0] == EXTENSIONS) "$EXTENSIONS/${it[1]}" else it[0] }
            .toSet()

    private fun prune(
        folder: Path,
        owned: String,
        keep: Set<String>,
    ) {
        val start = folder.resolve(owned)
        if (!Files.isDirectory(start, LinkOption.NOFOLLOW_LINKS)) return
        Files.walkFileTree(
            start,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    if (folder.relativize(file).joinToString("/") !in keep) Files.delete(file)
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(
                    dir: Path,
                    exc: IOException?,
                ): FileVisitResult {
                    if (dir != start && Files.list(dir).use { it.findAny().isEmpty }) Files.delete(dir)
                    return FileVisitResult.CONTINUE
                }
            },
        )
    }

    private fun packedConfig(pack: ContentPack): JsonObject {
        val config =
            try {
                JsonParser.parseString(pack.open(CONFIG_ENTRY).use { it.reader().readText() }).asJsonObject
            } catch (error: JsonParseException) {
                throw ContentPackException("$CONFIG_ENTRY is not valid JSON", error)
            } catch (error: IllegalStateException) {
                throw ContentPackException("$CONFIG_ENTRY is not a JSON object", error)
            }
        val extensions = config.get(EXTENSIONS)
        if (extensions != null && !extensions.isJsonObject) {
            throw ContentPackException("$CONFIG_ENTRY: extensions is not an object")
        }
        return config
    }

    /** The pack's `client.json`, plus the extensions the game's own names that the pack does not. */
    private fun mergedConfig(
        pack: ContentPack,
        folder: Path,
    ): JsonObject {
        val config = packedConfig(pack)
        val kept = ownerExtensions(folder.resolve(CONFIG))
        if (kept.size() == 0) return config
        val extensions = config.getAsJsonObject(EXTENSIONS) ?: JsonObject().also { config.add(EXTENSIONS, it) }
        for ((name, scripts) in kept.entrySet()) if (!extensions.has(name)) extensions.add(name, scripts)
        return config
    }

    private fun ownerExtensions(file: Path): JsonObject {
        if (!Files.isRegularFile(file)) return JsonObject()
        return try {
            JsonParser
                .parseString(Files.readString(file))
                .asJsonObject
                .get(EXTENSIONS)
                ?.takeIf { it.isJsonObject }
        } catch (_: JsonParseException) {
            null
        } catch (_: IllegalStateException) {
            null
        }?.asJsonObject ?: JsonObject()
    }

    private companion object {
        const val CONFIG = "client.json"
        const val CONFIG_ENTRY = "${ContentPack.CLIENT_PREFIX}$CONFIG"
        const val EXTENSIONS = "extensions"
        const val MIN_EXTENSION_PARTS = 3
        val GSON = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    }
}
