@file:Suppress("MagicNumber") // the code numbers the client reads

package zel.protocol.outgoing.catalog

enum class PurchaseError(
    val code: Int,
) {
    GENERIC(0),
    BADGE_OWNED(1),
    EFFECT_OWNED(5),
    CHAT_STYLE_OWNED(11),
    NO_SUCH_RECEIVER(14),
}
