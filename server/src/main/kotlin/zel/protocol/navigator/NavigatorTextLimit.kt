package zel.protocol.navigator

@JvmInline
internal value class NavigatorTextLimit(
    val max: Int,
) {
    fun clean(raw: String?): String = raw.orEmpty().trim().take(max)

    companion object {
        val SEARCH = NavigatorTextLimit(64)
        val SAVED_SEARCH = SEARCH
    }
}
