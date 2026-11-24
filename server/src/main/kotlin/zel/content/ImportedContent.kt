package zel.content

import zel.db.rows
import java.sql.Connection

/** What the last import left in `content_packs`, and whether this version can run on it. */
internal class ImportedContent private constructor(
    private val packs: List<Pack>,
) {
    private class Pack(
        val name: String,
        val version: String,
        val format: Int,
    )

    val present: Boolean get() = packs.isNotEmpty()

    val summary: String get() = packs.joinToString { "${it.name} ${it.version}" }

    /** Null when every imported pack has a format this version reads. */
    fun formatProblem(): String? =
        packs.firstOrNull { !ContentSections.reads(it.format) }?.let {
            "the imported content pack ${it.name} ${it.version} has format ${it.format}, this version reads " +
                "${ContentSections.supported()}: import a pack of format ${ContentSections.FORMAT_VERSION}"
        }

    companion object {
        const val NONE = "no content pack is imported: import one before starting the game"
        private const val HAS_TABLE =
            "SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'content_packs'"

        fun read(connection: Connection): ImportedContent {
            if (connection.rows(HAS_TABLE) { 1 }.isEmpty()) return ImportedContent(emptyList())
            return ImportedContent(
                connection.rows("SELECT name, version, format FROM content_packs ORDER BY name") {
                    Pack(it.getString("name"), it.getString("version"), it.getInt("format"))
                },
            )
        }
    }
}
