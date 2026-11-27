package zel.game.players.inventory

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.content.ContentDirs
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * The badge codes the game's texts name: a `badge_name_<code>` or `badge_name_al_<code>` line that is not empty, in
 * external_texts or its override file (a line there replaces the pack's). The files are read again when one changes,
 * looked at no more than every ten seconds.
 */
class BadgeTexts(
    private val files: () -> List<Path>,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private class Known(
        val stamp: String,
        val codes: Set<String>,
    )

    private var known = Known("", emptySet())
    private var lookedAt: Long? = null

    /** Whether the texts name exactly [code], as written. */
    fun names(code: String): Boolean = current().contains(code)

    @Synchronized
    private fun current(): Set<String> {
        val now = clock()
        val last = lookedAt
        if (last != null && now - last < RECHECK_MILLIS) return known.codes
        lookedAt = now
        val paths = files()
        val stamp = paths.joinToString("|") { "$it:${modified(it)}" }
        if (stamp != known.stamp) known = Known(stamp, read(paths))
        return known.codes
    }

    private fun read(paths: List<Path>): Set<String> {
        val lines = HashMap<String, String>()
        try {
            paths.filter(Files::isRegularFile).forEach { path ->
                Files.newInputStream(path).bufferedReader(Charsets.UTF_8).useLines { all ->
                    all.filter { it.startsWith(NAME) }.forEach { line ->
                        val split = line.indexOf('=')
                        if (split > 0) lines[line.substring(0, split)] = line.substring(split + 1)
                    }
                }
            }
        } catch (e: IOException) {
            log.warn("the badge names of the game's texts could not be read", e)
        }
        if (lines.isEmpty()) log.warn("the game's texts name no badge: only achievement, rank and VIP badges exist")
        return lines.filterValues { it.isNotBlank() }.keys.mapTo(HashSet()) { key ->
            key.removePrefix(NAME_ALIAS).takeIf { it != key } ?: key.removePrefix(NAME)
        }
    }

    private fun modified(path: Path): String =
        try {
            if (Files.isRegularFile(path)) "${Files.getLastModifiedTime(path).toMillis()}/${Files.size(path)}" else "-"
        } catch (_: IOException) {
            "-"
        }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BadgeTexts::class.java)
        private const val NAME = "badge_name_"
        private const val NAME_ALIAS = "badge_name_al_"
        private const val RECHECK_MILLIS = 10_000L
        private const val TEXTS = "external_texts"
        private const val OVERRIDES = "overrides/external_texts.txt"

        /** The texts the web serves the client: ZEL_GAMEDATA_DIR's external_texts, then its override file. */
        fun installed(): BadgeTexts = BadgeTexts(::installedFiles)

        private fun installedFiles(): List<Path> {
            val gamedata = ContentDirs.fromEnvironment().gamedata ?: return emptyList()
            return listOf(gamedata.resolve(TEXTS), gamedata.resolve(OVERRIDES))
        }
    }
}
