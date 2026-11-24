package zel.game.catalog

import java.util.concurrent.atomic.AtomicReference

/** The preview image an offer shows, by the offer's name; the content defines them. */
internal object OfferPreviewImages {
    private val images = AtomicReference<Map<String, String>>(emptyMap())

    fun install(all: Map<String, String>) {
        images.set(all)
    }

    fun of(localizationId: String?): String = images.get()[localizationId].orEmpty()
}
