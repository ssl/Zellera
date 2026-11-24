package zel.game.players

import zel.db.SqlFailure
import zel.game.navigation.BlockStyle
import zel.game.navigation.DisplayMode
import zel.server.GameServices
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(NavigatorLayouts::class.java)

private const val SAVE_SQL =
    "INSERT INTO player_navigator_sections (player_id, section, list_mode, collapsed) VALUES (?, ?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE list_mode = VALUES(list_mode), collapsed = VALUES(collapsed)"

/** The width of `player_navigator_sections.section`. */
private const val MAX_SECTION_LENGTH = 128

/** Far more sections than the navigator's views, categories and searches show one player. */
private const val MAX_SECTIONS = 200

private const val STORED_ROWS = "list"
private const val STORED_TILES = "thumbnails"

/** Section states are written in order off the caller's thread. */
class NavigatorLayouts(
    private val services: GameServices,
    private val playerId: Int,
    writeOn: (Runnable) -> Unit = { services.threading.run(it) },
) {
    private data class Layout(
        val style: BlockStyle = BlockStyle.ROWS,
        val display: DisplayMode = DisplayMode.VISIBLE,
    )

    private val layouts = HashMap<String, Layout>()
    private val writes = PlayerSaveQueue(writeOn)

    @Synchronized
    internal fun fromRow(
        section: String,
        storedStyle: String,
        collapsed: Boolean,
    ) {
        layouts[section] =
            Layout(
                if (storedStyle == STORED_TILES) BlockStyle.TILES else BlockStyle.ROWS,
                if (collapsed) DisplayMode.COLLAPSED else DisplayMode.VISIBLE,
            )
    }

    fun display(
        section: String,
        mode: DisplayMode,
    ) = change(section) { it.copy(display = mode) }

    fun setStyle(
        section: String,
        style: BlockStyle,
    ) = change(section) { it.copy(style = style) }

    @Synchronized
    fun displayOf(
        section: String,
        fallback: DisplayMode = DisplayMode.VISIBLE,
    ): DisplayMode = layouts[section]?.display ?: fallback

    @Synchronized
    fun styleOf(
        section: String,
        fallback: BlockStyle = BlockStyle.ROWS,
    ): BlockStyle = layouts[section]?.style ?: fallback

    /** Section names come from the client: blank, too long or excess ones are dropped. */
    private fun change(
        section: String,
        edit: (Layout) -> Layout,
    ) {
        if (section.isBlank() || section.length > MAX_SECTION_LENGTH) return
        val layout =
            synchronized(this) {
                val known = layouts[section]
                if (known == null && layouts.size >= MAX_SECTIONS) return
                edit(known ?: Layout()).also { layouts[section] = it }
            }
        writes.submit(playerId) { save(section, layout) }
    }

    private fun save(
        section: String,
        layout: Layout,
    ) {
        try {
            services.database.queries.write(SAVE_SQL) {
                int(playerId)
                string(section)
                string(if (layout.style == BlockStyle.TILES) STORED_TILES else STORED_ROWS)
                boolean(layout.display == DisplayMode.COLLAPSED)
            }
        } catch (e: SqlFailure) {
            log.error("navigator section {} of player {} not written", section, playerId, e)
        }
    }
}
