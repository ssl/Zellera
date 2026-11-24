package zel.content

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.HexFormat
import java.util.zip.ZipException
import java.util.zip.ZipFile

@Suppress("ThrowsCount") // one refusal per check
internal class ContentPack private constructor(
    private val zip: ZipFile,
    val name: String,
    val version: String,
    val format: Int,
    val sha256: String,
    val sections: List<DeclaredSection>,
    val files: List<String>,
    val assets: PackAssets,
) : Closeable {
    override fun close() = zip.close()

    fun open(entry: String): InputStream = zip.getInputStream(zip.getEntry(entry))

    fun readRows(
        section: DeclaredSection,
        onColumns: (List<String>) -> Unit,
        onRow: (JsonArray) -> Unit,
    ) = open(section.entry).use { Rows(section.entry, it).readInto(onColumns, onRow) }

    fun clientStrings(): Map<String, String> {
        if (PROFILE_ENTRY !in files) return emptyMap()
        val strings =
            jsonObjectOf(open(PROFILE_ENTRY).use { it.reader().readText() }, PROFILE_ENTRY)
                .objectAt("strings", PROFILE_ENTRY)
        return strings.entrySet().associate { (key, value) ->
            if (!value.isJsonPrimitive) throw ContentPackException("$PROFILE_ENTRY: $key is not a string")
            key to value.asString
        }
    }

    private fun countRows(section: DeclaredSection) {
        var counted = 0L
        readRows(section, {}) { counted++ }
        if (counted != section.rows) {
            throw ContentPackException("${section.entry} holds $counted rows, the manifest says ${section.rows}")
        }
    }

    private class Manifest(
        private val root: JsonObject,
    ) {
        fun format(): Int {
            val format = root.get("format")
            if (format == null || !format.isJsonPrimitive || !format.asJsonPrimitive.isNumber) {
                throw ContentPackException("the manifest has no format version")
            }
            if (!ContentSections.reads(format.asInt)) {
                throw ContentPackException(
                    "pack format ${format.asInt} is not supported; this server reads ${ContentSections.supported()}",
                )
            }
            return format.asInt
        }

        fun text(key: String): String {
            val value =
                root
                    .get(key)
                    ?.takeIf { it.isJsonPrimitive }
                    ?.asString
                    .orEmpty()
            if (value.isBlank()) throw ContentPackException("the manifest has no $key")
            return value
        }

        fun sections(): List<DeclaredSection> {
            val declared =
                sectionsObject().keySet().map { id ->
                    val section =
                        ContentSections.BY_ID[id] ?: throw ContentPackException("unknown section '$id' in the manifest")
                    val declaration = sectionsObject().objectAt(id, "sections")
                    val rows = declaration.countAt("rows", "section $id")
                    DeclaredSection(section, "$TABLES_PREFIX$id$TABLES_SUFFIX", rows)
                }
            val lacking = ContentSections.REQUIRED - declared.map { it.section.id }.toSet()
            if (lacking.isNotEmpty()) {
                throw ContentPackException("the pack lacks the required sections ${lacking.sorted()}")
            }
            return declared
        }

        fun files(): List<String> {
            if (root.get("files") == null) return emptyList()
            return root
                .objectAt("files", MANIFEST)
                .keySet()
                .toList()
                .onEach(::checkFilePath)
        }

        private fun checkFilePath(entry: String) {
            if (entry.startsWith("/") || entry.contains('\\') || entry.split('/').contains("..")) {
                throw ContentPackException("unsafe path in the manifest: $entry")
            }
            if (FILE_PREFIXES.none(entry::startsWith)) {
                throw ContentPackException("unknown file in the manifest: $entry")
            }
        }

        fun checksums(
            sections: List<DeclaredSection>,
            files: List<String>,
        ): Map<String, String> {
            val tables = sectionsObject()
            val stated = LinkedHashMap<String, String>()
            sections.associateTo(stated) {
                it.entry to tables.objectAt(it.section.id, MANIFEST).textAt("sha256", "section ${it.section.id}")
            }
            files.associateTo(stated) { it to root.objectAt("files", MANIFEST).textAt(it, "files") }
            return stated
        }

        fun assets(): PackAssets {
            val assets = root.objectAt("assets", MANIFEST)
            val dir = assets.optionalTextAt("dir", "assets")
            val url = assets.optionalTextAt("url", "assets")
            return when (val mode = assets.optionalTextAt("mode", "assets")) {
                "included" -> PackAssets(inPack = true, dir = "", url = "")
                "location" -> PackAssets(inPack = false, dir = dir, url = url)
                else -> throw ContentPackException("assets mode '$mode' is unknown; expected included or location")
            }
        }

        private fun sectionsObject(): JsonObject = root.objectAt("sections", MANIFEST)
    }

    /** `tables/<section>.jsonl`: a columns header line, then one JSON array per row. */
    private class Rows(
        private val entry: String,
        input: InputStream,
    ) {
        private val reader = input.bufferedReader()

        fun readInto(
            onColumns: (List<String>) -> Unit,
            onRow: (JsonArray) -> Unit,
        ) {
            val header = reader.readLine() ?: throw ContentPackException("$entry is empty")
            val columns =
                jsonObjectOf(header, entry)
                    .get("columns")
                    ?.takeIf { it.isJsonArray }
                    ?.asJsonArray
                    ?.map { column -> column.takeIf { it.isJsonPrimitive }?.asString ?: refuse(entry, "text columns") }
            if (columns.isNullOrEmpty()) throw ContentPackException("$entry has no columns header")
            onColumns(columns)
            reader.lineSequence().forEachIndexed { index, line -> onRow(rowOf(line, index + 2, columns.size)) }
        }

        private fun rowOf(
            line: String,
            lineNumber: Int,
            width: Int,
        ): JsonArray {
            val row =
                try {
                    JsonParser.parseString(line).asJsonArray
                } catch (error: JsonParseException) {
                    throw ContentPackException("$entry line $lineNumber is not a row", error)
                } catch (error: IllegalStateException) {
                    throw ContentPackException("$entry line $lineNumber is not a row", error)
                }
            if (row.size() != width) {
                throw ContentPackException("$entry line $lineNumber has ${row.size()} values for $width columns")
            }
            return row
        }
    }

    companion object {
        const val MANIFEST = "manifest.json"
        const val PROFILE_ENTRY = "client/profile.json"

        val READ_ENTRIES = setOf(PROFILE_ENTRY)
        private const val TABLES_PREFIX = "tables/"
        private const val TABLES_SUFFIX = ".jsonl"
        private val FILE_PREFIXES = listOf("clientdata/", "badges/parts/", "snowball/arenas/", "assets/", "client/")

        fun open(path: Path): ContentPack {
            if (!Files.isRegularFile(path)) throw ContentPackException("no such pack: $path")
            val zip =
                try {
                    ZipFile(path.toFile())
                } catch (error: ZipException) {
                    throw ContentPackException("$path is not a zip file", error)
                }
            try {
                return read(zip, path)
            } catch (error: ContentPackException) {
                zip.close()
                throw error
            } catch (error: IOException) {
                zip.close()
                throw ContentPackException("$path cannot be read: ${error.message}", error)
            }
        }

        private fun read(
            zip: ZipFile,
            path: Path,
        ): ContentPack {
            val manifestEntry = zip.getEntry(MANIFEST) ?: throw ContentPackException("$path has no $MANIFEST")
            val manifestText = zip.getInputStream(manifestEntry).use { it.reader().readText() }
            val manifest = Manifest(jsonObjectOf(manifestText, MANIFEST))
            val format = manifest.format()
            val name = manifest.text("name")
            val version = manifest.text("version")
            val sections = manifest.sections()
            val files = manifest.files()
            checkEntries(zip, manifest.checksums(sections, files))
            val pack = ContentPack(zip, name, version, format, sha256Of(path), sections, files, manifest.assets())
            sections.forEach(pack::countRows)
            pack.clientStrings()
            return pack
        }

        /** Every stated entry must be present with its checksum, and nothing else may be in the zip. */
        private fun checkEntries(
            zip: ZipFile,
            stated: Map<String, String>,
        ) {
            val present =
                zip
                    .entries()
                    .asSequence()
                    .map { it.name }
                    .filterNot { it.endsWith("/") }
                    .toSet() - MANIFEST
            val undeclared = present - stated.keys
            if (undeclared.isNotEmpty()) {
                throw ContentPackException("files the manifest does not declare: ${undeclared.minOrNull()} and others")
            }
            for ((entry, sha) in stated) {
                val zipEntry = zip.getEntry(entry) ?: throw ContentPackException("missing from the pack: $entry")
                val actual = zip.getInputStream(zipEntry).use(::sha256Of)
                if (!actual.equals(sha, ignoreCase = true)) throw ContentPackException("checksum mismatch: $entry")
            }
        }

        private fun jsonObjectOf(
            text: String,
            what: String,
        ): JsonObject =
            try {
                JsonParser.parseString(text).asJsonObject
            } catch (error: JsonParseException) {
                throw ContentPackException("$what is not valid JSON", error)
            } catch (error: IllegalStateException) {
                throw ContentPackException("$what is not a JSON object", error)
            }

        private fun sha256Of(input: InputStream): String {
            val hash = MessageDigest.getInstance("SHA-256")
            DigestInputStream(input, hash).use { it.transferTo(OutputStream.nullOutputStream()) }
            return HexFormat.of().formatHex(hash.digest())
        }

        private fun sha256Of(path: Path): String = Files.newInputStream(path).use(::sha256Of)
    }
}

private fun refuse(
    what: String,
    expected: String,
): Nothing = throw ContentPackException("$what is not $expected")

private fun JsonObject.objectAt(
    key: String,
    what: String,
): JsonObject = get(key)?.takeIf { it.isJsonObject }?.asJsonObject ?: refuse("$key in $what", "an object")

private fun JsonObject.textAt(
    key: String,
    what: String,
): String = get(key)?.takeIf { it.isJsonPrimitive }?.asString ?: refuse("$key in $what", "a text")

private fun JsonObject.optionalTextAt(
    key: String,
    what: String,
): String = if (get(key) == null) "" else textAt(key, what)

private fun JsonObject.countAt(
    key: String,
    what: String,
): Long =
    get(key)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.asNumber
        ?.toString()
        ?.toLongOrNull() ?: refuse("$key in $what", "a whole number")
