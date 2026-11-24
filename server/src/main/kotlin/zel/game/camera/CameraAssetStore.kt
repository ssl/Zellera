package zel.game.camera

import org.slf4j.LoggerFactory
import zel.db.Database
import zel.db.Sql
import zel.db.SqlFailure
import java.sql.ResultSet

private val log = LoggerFactory.getLogger(CameraAssetStore::class.java)

class CameraAssetStore(
    private val database: Database,
) {
    internal fun load() {
        try {
            val sql = Sql(database.dataSource)
            val filters =
                sql
                    .select("SELECT name, kind, blend, matrix FROM camera_filters")
                    .rows { row -> filterOf(row) }
                    .filterNotNull()
                    .toMap()
            val libraries =
                sql.select("SELECT name, base, shades FROM camera_libraries").rows {
                    CameraAssetData.Library(
                        it.getString("name"),
                        it.getBoolean("base"),
                        shadesOf(it.getString("shades")),
                    )
                }
            CameraAssetData.install(filters, libraries)
        } catch (e: SqlFailure) {
            log.error("camera filters and libraries not read; photos render without them", e)
        }
    }

    @Suppress("TooGenericExceptionCaught") // one bad row leaves out that filter only
    private fun filterOf(row: ResultSet): Pair<String, PhotoFilter>? {
        val name = row.getString("name")
        return try {
            val filter: PhotoFilter =
                when (row.getString("kind")) {
                    "matrix" -> PhotoFilters.matrix(row.getString("matrix"))
                    "overlay" -> PhotoFilter.Overlay(BlendMode.valueOf(row.getString("blend").uppercase()))
                    else -> PhotoFilter.Border
                }
            name to filter
        } catch (e: RuntimeException) {
            log.error("camera filter {} is left out: {}", name, e.message)
            null
        }
    }

    private fun shadesOf(text: String?): Map<Int, Int> =
        text
            .orEmpty()
            .split(',')
            .filter { it.isNotBlank() }
            .associate { it.substringBefore(':').trim().toInt() to it.substringAfter(':').trim().toInt() }
}
