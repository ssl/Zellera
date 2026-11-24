package zel.game.landing

data class NewsArticle(
    val id: Int,
    val title: String,
    val body: String,
    val buttonText: String,
    val link: NewsLink,
    val image: String,
)
