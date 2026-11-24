package zel.game.featuredlooks

import zel.core.Configuration
import zel.db.Sql
import zel.db.SqlFailure
import zel.util.MILLIS_PER_SECOND
import org.slf4j.LoggerFactory
import java.util.Random
import java.util.concurrent.ThreadLocalRandom
import javax.sql.DataSource

/** The rows are kept for `featured_looks.cache.seconds`, so an edit shows up without a restart. */
class FeaturedLooks(
    dataSource: DataSource,
    private val settings: Configuration,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val queries = Sql(dataSource)

    private class Pool(
        val looks: List<FeaturedLook>,
        val readAt: Long,
    )

    @Volatile private var pool = readPool()

    fun draw(count: Int): List<FeaturedLook> = draw(currentPool().looks, count, ThreadLocalRandom.current())

    @Synchronized
    private fun currentPool(): Pool {
        val maxAge = settings.getInt("featured_looks.cache.seconds", DEFAULT_CACHE_SECONDS) * MILLIS_PER_SECOND
        if (nowMillis() - pool.readAt > maxAge) pool = readPool()
        return pool
    }

    private fun readPool(): Pool {
        val looks =
            try {
                queries.select(SELECT).rows { look(it.getString("gender"), it.getString("figure")) }.filterNotNull()
            } catch (failure: SqlFailure) {
                LOGGER.error("Reading the featured looks failed", failure)
                emptyList()
            }
        return Pool(looks, nowMillis())
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(FeaturedLooks::class.java)
        private const val DEFAULT_CACHE_SECONDS = 300
        private const val SELECT = "SELECT figure, gender FROM featured_looks WHERE enabled = 1"

        internal fun look(
            gender: String?,
            figure: String?,
        ): FeaturedLook? = figure?.takeUnless { it.isBlank() }?.let { FeaturedLook(FeaturedLook.genderOf(gender), it) }

        fun draw(
            looks: List<FeaturedLook>,
            count: Int,
            random: Random,
        ): List<FeaturedLook> = looks.shuffled(random).take(maxOf(0, count))
    }
}
