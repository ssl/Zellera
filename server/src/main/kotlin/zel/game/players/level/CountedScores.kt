package zel.game.players.level

data class CountedScores(
    val achievements: Int,
    val badges: Int,
    val hosting: Int,
    val furni: Int,
) {
    val total: Int get() = achievements + badges + hosting + furni

    companion object {
        fun of(
            achievements: Int,
            badges: Int,
            hosting: Int,
            furni: Int,
        ): CountedScores {
            val countedBadges = minOf(badges, achievements)
            val countedHosting = minOf(hosting, achievements + countedBadges)
            val countedFurni = minOf(furni, achievements + countedBadges + countedHosting)
            return CountedScores(achievements, countedBadges, countedHosting, countedFurni)
        }
    }
}
