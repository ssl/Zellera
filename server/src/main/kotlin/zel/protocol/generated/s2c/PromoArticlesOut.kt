package zel.protocol.generated

data class PromoArticlesOut(
    val articles: List<ArticlesItem>,
) {
    data class ArticlesItem(
        val id: Int,
        val title: String,
        val message: String,
        val buttonMessage: String,
        val linkType: Int,
        val link: String,
        val image: String,
    )
}

object PromoArticlesOutCodec : OutgoingCodec<PromoArticlesOut> {
    override val name = "PromoArticlesOut"

    override fun read(r: PacketReader): PromoArticlesOut = readPromoArticlesOut(r)

    override fun write(w: PacketWriter, m: PromoArticlesOut) = writePromoArticlesOut(w, m)

    private fun readPromoArticlesOutArticlesItem(r: PacketReader): PromoArticlesOut.ArticlesItem {
        val vId = r.int()
        val vTitle = r.string()
        val vMessage = r.string()
        val vButtonMessage = r.string()
        val vLinkType = r.int()
        val vLink = r.string()
        val vImage = r.string()
        return PromoArticlesOut.ArticlesItem(vId, vTitle, vMessage, vButtonMessage, vLinkType, vLink, vImage)
    }

    private fun readPromoArticlesOut(r: PacketReader): PromoArticlesOut {
        val vArticles = List(r.countInt(18)) { readPromoArticlesOutArticlesItem(r) }
        return PromoArticlesOut(vArticles)
    }

    private fun writePromoArticlesOutArticlesItem(w: PacketWriter, m: PromoArticlesOut.ArticlesItem) {
        w.int(m.id)
        w.string(m.title)
        w.string(m.message)
        w.string(m.buttonMessage)
        w.int(m.linkType)
        w.string(m.link)
        w.string(m.image)
    }

    private fun writePromoArticlesOut(w: PacketWriter, m: PromoArticlesOut) {
        w.countInt(m.articles.size)
        m.articles.forEach { writePromoArticlesOutArticlesItem(w, it) }
    }
}
