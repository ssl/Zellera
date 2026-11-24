package zel.game.featuredlooks

import zel.game.players.Gender

data class FeaturedLook(
    val gender: Gender,
    val figure: String,
) {
    /** The client takes the gender in lower case. */
    val genderCode: String get() = gender.name.lowercase()

    companion object {
        /** Any spelling starting with F is female; everything else, missing included, is male. */
        fun genderOf(spelling: String?): Gender =
            if (spelling?.trim()?.firstOrNull()?.uppercaseChar() == 'F') Gender.F else Gender.M
    }
}
