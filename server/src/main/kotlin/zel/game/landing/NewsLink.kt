package zel.game.landing

data class NewsLink(
    val type: NewsLinkType,
    val target: String,
) {
    companion object {
        /** Catalog page to `catalog/open/<name>`, room id to `navigator/goto/<id>`, site path to a URL on [site]. */
        fun of(
            kind: String?,
            link: String?,
            site: String,
        ): NewsLink {
            val value = link.orEmpty()
            return when (kind) {
                "web" -> NewsLink(NewsLinkType.WEB, if (value.startsWith("/")) site + value else value)
                "client" -> NewsLink(NewsLinkType.CLIENT, value)
                "catalog" -> NewsLink(NewsLinkType.CLIENT, "catalog/open/$value")
                "room" -> NewsLink(NewsLinkType.CLIENT, "navigator/goto/$value")
                else -> NewsLink(NewsLinkType.NONE, "")
            }
        }
    }
}
