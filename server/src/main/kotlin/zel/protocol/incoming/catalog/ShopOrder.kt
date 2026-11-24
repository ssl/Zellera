package zel.protocol.incoming.catalog

data class ShopOrder(
    val pageId: Int,
    val itemId: Int,
    val extraData: String,
    val count: Int,
) {
    val namesNoPage: Boolean get() = pageId == SEARCH_PAGE_ID || pageId == NO_PAGE_ID

    private companion object {
        const val SEARCH_PAGE_ID = -12345678
        const val NO_PAGE_ID = -1
    }
}
