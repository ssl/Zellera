package zel.game.items

import zel.core.threading.GameThreads
import zel.db.Sql
import zel.db.orFallback
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

/** The playlists every television furni shows; the key is ZEL_YOUTUBE_API_KEY of the operator's config. */
class YoutubePlaylists(
    dataSource: DataSource,
    private val threading: GameThreads,
    private val apiKey: String = System.getenv(API_KEY).orEmpty().trim(),
) {
    private val queries = Sql(dataSource)

    @Volatile
    private var playlists: List<YoutubePlaylist> = emptyList()

    fun all(): List<YoutubePlaylist> = playlists

    fun load() {
        val codes = listed()
        if (codes.isEmpty()) {
            playlists = emptyList()
            return
        }
        if (apiKey.isEmpty()) {
            LOGGER.info("{} TV playlists listed, but no {}: the TVs stay empty", codes.size, API_KEY)
            playlists = emptyList()
            return
        }
        val fetched = fetch(YoutubeApi(apiKey), codes)
        playlists = codes.mapNotNull(fetched::get)
        LOGGER.info("{} of {} TV playlists fetched", fetched.size, codes.size)
    }

    private fun listed(): List<String> =
        LOGGER.orFallback("reading the TV playlists", emptyList()) {
            queries.select(LISTED_SQL).rows { row -> row.getString("playlist_code") }
        }

    private fun fetch(
        api: YoutubeApi,
        codes: List<String>,
    ): Map<String, YoutubePlaylist> {
        val fetched = ConcurrentHashMap<String, YoutubePlaylist>()
        val done = CountDownLatch(codes.size)
        codes.forEach { code ->
            val queued =
                threading.run {
                    try {
                        api.playlist(code)?.let { fetched[code] = it }
                    } finally {
                        done.countDown()
                    }
                }
            if (queued == null) done.countDown()
        }
        try {
            if (!done.await(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                LOGGER.warn("TV playlists still fetching after {} s", FETCH_TIMEOUT_SECONDS)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return fetched
    }

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(YoutubePlaylists::class.java)
        const val API_KEY = "ZEL_YOUTUBE_API_KEY"
        const val LISTED_SQL = "SELECT playlist_code FROM youtube_playlists ORDER BY position"
        const val FETCH_TIMEOUT_SECONDS = 30L
    }
}
