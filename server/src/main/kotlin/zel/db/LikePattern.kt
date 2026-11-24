package zel.db

object LikePattern {
    private val SPECIAL = Regex("""[\\%_]""")

    fun startingWith(text: String?): String = "${literal(text)}%"

    fun containing(text: String?): String = "%${literal(text)}%"

    private fun literal(text: String?): String = text.orEmpty().replace(SPECIAL) { "\\" + it.value }
}
