package zel.game.catalog

import zel.protocol.BodyPart
import zel.protocol.generated.PacketWriter

class FeaturedPageSlot(
    private val slotId: Int,
    private val caption: String?,
    private val image: String?,
    private val link: FeaturedLink,
    private val expiresAt: Int?,
    private val services: CatalogServices,
) : BodyPart {
    override fun writeTo(body: PacketWriter) {
        body.int(slotId)
        body.string(caption.orEmpty())
        body.string(image.orEmpty())
        body.int(link.wireType)
        link.writeTarget(body)
        body.int(secondsLeft(expiresAt, services.now()))
    }

    companion object {
        internal fun secondsLeft(
            expiresAt: Int?,
            now: Int,
        ): Int = if (expiresAt == null) now + 1 else maxOf(expiresAt - now, 1)
    }
}
