package zel.game.catalog.layouts

import zel.game.catalog.MembershipOffer
import java.sql.ResultSet

/** One row of `catalogue_layouts`: what a page layout is called, how it is sent and what the server does with it. */
data class PageLayoutRow(
    val code: String,
    val wireCode: String,
    val kind: Kind = Kind.FIELDS,
    val role: PageForm.Role = PageForm.Role.NONE,
    val window: MembershipOffer.Window = MembershipOffer.Window.CLUB,
    val images: String = "",
    val texts: String = "",
    val listsFeatured: Boolean = false,
    val needs: Needs = Needs.NONE,
) {
    enum class Kind { FIELDS, CLUB_BUY, BUILD_PASS, FOLDER }

    /** A layout that is only offered while the game rule behind it is on. */
    enum class Needs { NONE, RECYCLER, MARKETPLACE }

    fun form(): PageForm =
        when (kind) {
            Kind.FIELDS -> {
                PageForm.Fields(
                    wireCode,
                    PageSlots.images(images),
                    PageSlots.texts(texts),
                    listsFeatured,
                    role,
                )
            }

            Kind.CLUB_BUY -> {
                PageForm.ClubBuy(wireCode)
            }

            Kind.BUILD_PASS -> {
                PageForm.BuildPass(wireCode, window)
            }

            Kind.FOLDER -> {
                PageForm.Folder
            }
        }

    companion object {
        fun read(row: ResultSet): PageLayoutRow =
            PageLayoutRow(
                code = row.getString("code").lowercase(),
                wireCode = row.getString("wire_code"),
                kind = enumOf(row.getString("kind")),
                role = PageForm.Role.entries.firstOrNull { it.code == row.getString("role") } ?: PageForm.Role.NONE,
                window = enumOf(row.getString("membership_window")),
                images = row.getString("images").orEmpty(),
                texts = row.getString("texts").orEmpty(),
                listsFeatured = row.getBoolean("lists_featured"),
                needs = enumOf(row.getString("needs")),
            )

        private inline fun <reified E : Enum<E>> enumOf(value: String): E =
            enumValues<E>().first { it.name.equals(value, ignoreCase = true) }
    }
}
