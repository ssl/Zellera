package zel.ext

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path

data class ExtensionManifest(
    val name: String,
    val version: String,
    val description: String,
    val jar: String,
    val main: String,
    val server: String,
    val settings: Map<String, String>,
) {
    companion object {
        const val FILE = "extension.json"

        private val NAME = Regex("^[a-z][a-z0-9]{1,31}$")
        private val VERSION = Regex("^\\d{1,4}\\.\\d{1,4}\\.\\d{1,6}$")
        private val JAR = Regex("^[A-Za-z0-9][A-Za-z0-9_.-]{0,80}\\.jar$")
        private val CLASS = Regex("^[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+$")
        private val KEY = Regex("^[a-z][a-z0-9_.]{0,63}$")
        private const val MAX_TEXT = 200

        fun read(folder: Path): ExtensionManifest {
            val file = folder.resolve(FILE)
            need(Files.isRegularFile(file)) { "$folder has no $FILE" }
            val json =
                runCatching { JsonParser.parseString(Files.readString(file)).asJsonObject }
                    .getOrElse { fail("$file is not a JSON object: ${it.message}") }
            val name = text(json, "name", file)
            need(NAME.matches(name)) { "$file: name must be a-z and 0-9, 2 to 32 characters" }
            need(name == folder.fileName.toString()) { "$file: name $name differs from its folder" }
            val version = text(json, "version", file)
            need(VERSION.matches(version)) { "$file: version must look like 1.0.0" }
            val jar = text(json, "jar", file)
            need(JAR.matches(jar)) { "$file: jar must be a .jar file name inside the folder" }
            val main = text(json, "main", file)
            need(CLASS.matches(main)) { "$file: main must be a class name such as org.example.Main" }
            val description = json.get("description")?.asString.orEmpty()
            need(description.length <= MAX_TEXT) { "$file: description is longer than $MAX_TEXT" }
            val server = json.get("server")?.asString.orEmpty()
            need(server.isEmpty() || ServerVersions.holds("1.0.0", server) != null) {
                "$file: server must list versions such as >=1.0.0 <2.0.0"
            }
            return ExtensionManifest(name, version, description, jar, main, server, settings(json, file))
        }

        private fun text(
            json: JsonObject,
            key: String,
            file: Path,
        ): String =
            json
                .get(key)
                ?.takeIf { it.isJsonPrimitive }
                ?.asString
                ?.trim()
                ?.takeIf(String::isNotEmpty) ?: fail("$file: $key is missing")

        private fun settings(
            json: JsonObject,
            file: Path,
        ): Map<String, String> {
            val settings = json.get("settings") ?: return emptyMap()
            need(settings.isJsonObject) { "$file: settings must be an object" }
            return settings.asJsonObject.entrySet().associate { (key, value) ->
                need(KEY.matches(key) && value.isJsonPrimitive) {
                    "$file: setting $key must be a lower-case key with a plain value"
                }
                key to value.asString
            }
        }
    }
}

class ExtensionError(
    message: String,
) : RuntimeException(message)

internal fun fail(message: String): Nothing = throw ExtensionError(message)

internal inline fun need(
    holds: Boolean,
    message: () -> String,
) {
    if (!holds) fail(message())
}

internal object ServerVersions {
    private val PART = Regex("^(>=|<=|>|<|=)?(\\d{1,4})\\.(\\d{1,4})\\.(\\d{1,6})$")

    fun holds(
        version: String,
        range: String,
    ): Boolean? {
        val running = PART.matchEntire(version)?.let(::numbers) ?: return null
        val parts = range.trim().split(Regex("\\s+")).map { PART.matchEntire(it) ?: return null }
        return parts.all { part ->
            val diff = compare(running, numbers(part))
            when (part.groupValues[1]) {
                ">=" -> diff >= 0
                "<=" -> diff <= 0
                ">" -> diff > 0
                "<" -> diff < 0
                else -> diff == 0
            }
        }
    }

    private fun numbers(match: MatchResult): List<Int> = match.groupValues.drop(2).map(String::toInt)

    private fun compare(
        a: List<Int>,
        b: List<Int>,
    ): Int = a.zip(b).map { (x, y) -> x - y }.firstOrNull { it != 0 } ?: 0
}
