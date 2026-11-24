package zel.game.catalog.layouts

import zel.game.catalog.BuildPassBuyPages
import zel.game.catalog.CatalogPage
import zel.game.catalog.MembershipOffer
import zel.protocol.generated.PacketWriter

sealed interface PageForm {
    val listsFeaturedPages: Boolean get() = false

    val role: Role get() = Role.NONE

    /** What the server does with a page beyond writing it. */
    enum class Role(
        val code: String,
    ) {
        NONE("none"),
        CLUB_GIFT("club_gift"),
        ROOM_ADS("room_ads"),
        ROOM_BUNDLE("room_bundle"),
    }

    fun write(
        body: PacketWriter,
        page: CatalogPage,
    )

    open class Fields(
        private val code: String,
        private val images: (CatalogPage) -> List<String>,
        private val texts: (CatalogPage) -> List<String>,
        override val listsFeaturedPages: Boolean = false,
        override val role: Role = Role.NONE,
    ) : PageForm {
        override fun write(
            body: PacketWriter,
            page: CatalogPage,
        ) {
            body.string(code)
            writeStrings(body, images(page))
            writeStrings(body, texts(page))
        }

        private fun writeStrings(
            body: PacketWriter,
            values: List<String>,
        ) {
            body.int(values.size)
            values.forEach { body.string(it) }
        }
    }

    class ClubBuy(
        code: String,
    ) : Fields(code, { listOf(it.info.images.header, it.info.images.teaser) }, { emptyList() })

    class BuildPass(
        private val code: String,
        val window: MembershipOffer.Window,
    ) : PageForm {
        override fun write(
            body: PacketWriter,
            page: CatalogPage,
        ) = BuildPassBuyPages.writePage(body, page, code)
    }

    /** Groups pages in the index; it has no page of its own to open. */
    data object Folder : PageForm {
        override fun write(
            body: PacketWriter,
            page: CatalogPage,
        ) = Unit
    }

    /** The invisible root of the page tree; it is never sent. */
    data object Root : PageForm {
        override fun write(
            body: PacketWriter,
            page: CatalogPage,
        ) = Unit
    }
}
