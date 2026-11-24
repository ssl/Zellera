package zel.game.catalog

import zel.protocol.generated.PacketWriter

sealed interface FeaturedLink {
    val wireType: Int

    fun writeTarget(body: PacketWriter)

    data class ToPageName(
        val pageName: String?,
    ) : FeaturedLink {
        override val wireType: Int get() = 0

        override fun writeTarget(body: PacketWriter) {
            body.string(pageName.orEmpty())
        }
    }

    data class ToPageId(
        val pageId: Int,
    ) : FeaturedLink {
        override val wireType: Int get() = 1

        override fun writeTarget(body: PacketWriter) {
            body.int(pageId)
        }
    }

    data class ToProduct(
        val productName: String?,
    ) : FeaturedLink {
        override val wireType: Int get() = 2

        override fun writeTarget(body: PacketWriter) {
            body.string(productName.orEmpty())
        }
    }

    companion object {
        fun of(
            type: String,
            pageName: String?,
            pageId: Int,
            productName: String?,
        ): FeaturedLink =
            when (type.uppercase()) {
                "PAGE_NAME" -> ToPageName(pageName)
                "PAGE_ID" -> ToPageId(pageId)
                "PRODUCT_NAME" -> ToProduct(productName)
                else -> throw IllegalArgumentException("Unknown featured page type: $type")
            }
    }
}
