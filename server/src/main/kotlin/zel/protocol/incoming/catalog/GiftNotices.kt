package zel.protocol.incoming.catalog

internal object GiftNotices {
    fun of(text: String): Map<String, String> = mapOf("message" to text)
}
