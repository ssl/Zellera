package zel.game.catalog

import org.slf4j.LoggerFactory
import zel.db.Database
import zel.db.Sql
import zel.db.SqlFailure

private val log = LoggerFactory.getLogger(OfferPreviewImageStore::class.java)

internal class OfferPreviewImageStore(
    private val database: Database,
) {
    fun readAll(): Map<String, String> =
        try {
            Sql(database.dataSource)
                .select("SELECT offer_name, image FROM offer_preview_images")
                .rows { it.getString("offer_name") to it.getString("image") }
                .toMap()
        } catch (e: SqlFailure) {
            log.error("offer preview images not read; offers show no preview", e)
            emptyMap()
        }
}
