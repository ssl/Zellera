package zel.game.landing

import zel.core.Configuration
import zel.db.Database
import zel.db.SqlFailure
import zel.game.items.ItemDataUrls
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NewsArticles(
    database: Database,
    private val configuration: Configuration,
) {
    private val sql = database.queries

    @Volatile
    var all: List<NewsArticle> = emptyList()
        private set

    init {
        refresh()
    }

    /** When the read fails the last list stays. */
    fun refresh() {
        val buttons = ItemDataUrls.site(configuration)
        val images = ItemDataUrls.site(configuration)
        all =
            try {
                sql.select(LIVE_IN_ORDER).rows { row ->
                    NewsArticle(
                        row.getInt("id"),
                        row.getString("title"),
                        row.getString("body"),
                        row.getString("button_text"),
                        NewsLink.of(row.getString("button_kind"), row.getString("button_link"), buttons),
                        ItemDataUrls.image(row.getString("image"), images),
                    )
                }
            } catch (cause: SqlFailure) {
                log.error("news articles not reloaded", cause)
                return
            }
    }

    private companion object {
        const val LIVE_IN_ORDER =
            "SELECT id, title, body, button_text, button_kind, button_link, image FROM news_articles " +
                "WHERE deleted = 0 ORDER BY sort_order, id"
        val log: Logger = LoggerFactory.getLogger(NewsArticles::class.java)
    }
}
