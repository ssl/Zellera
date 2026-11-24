package zel.game.catalog

import zel.db.intOrNone
import java.sql.ResultSet
import java.sql.SQLException

data class PageInfo(
    val id: Int = 0,
    val parentId: Int = 0,
    val minRank: Int = 0,
    val caption: String = "",
    val code: String = "",
    val icon: Int = 0,
    val position: Int = 0,
    val visible: Boolean = false,
    val enabled: Boolean = false,
    val clubOnly: Boolean = false,
    val templateRoomId: Int = 0,
    val shop: CatalogMode = CatalogMode.NORMAL,
    val layout: String = "",
    val images: Images = Images(),
    val wording: Wording = Wording(),
    val endsAt: Int? = null,
    val expiryImage: String = "",
) {
    data class Images(
        val header: String = "",
        val teaser: String = "",
        val special: String = "",
    )

    data class Wording(
        val one: String = "",
        val two: String = "",
        val details: String = "",
        val teaser: String = "",
    )

    companion object {
        const val ROOT_ID = -1
        private const val BUILD_PASS_CATALOGUE = "build_pass"

        val ROOT =
            PageInfo(
                id = ROOT_ID,
                parentId = -2,
                caption = "root",
                code = "root",
                position = -10,
                visible = true,
                enabled = true,
            )

        /** Columns a narrower query leaves out fall back to their defaults. */
        fun read(row: ResultSet): PageInfo {
            val text = { column: String -> row.getString(column).orEmpty() }
            return PageInfo(
                id = row.getInt("id"),
                parentId = row.intOrNone("parent_id", ROOT_ID),
                minRank = row.getInt("min_rank"),
                caption = text("caption"),
                code = text("code"),
                icon = row.getInt("icon_image"),
                position = row.getInt("sort_order"),
                visible = row.getBoolean("visible"),
                enabled = row.getBoolean("enabled"),
                clubOnly = row.getBoolean("club_only"),
                templateRoomId = whenSelected { row.getInt("room_id") } ?: 0,
                shop = shopOf(whenSelected { row.getString("catalogue") }),
                layout = text("layout"),
                images = Images(text("headline"), text("teaser"), text("special_image")),
                wording = Wording(text("text_1"), text("text_2"), text("text_details"), text("text_teaser")),
                endsAt = whenSelected { row.getInt("ends_at").takeIf { it > 0 } },
                expiryImage = whenSelected { row.getString("expiry_image") }.orEmpty(),
            )
        }

        private fun shopOf(catalogue: String?): CatalogMode =
            if (catalogue == BUILD_PASS_CATALOGUE) CatalogMode.BUILDER else CatalogMode.NORMAL

        private inline fun <T> whenSelected(read: () -> T): T? =
            try {
                read()
            } catch (_: SQLException) {
                null
            }
    }
}
