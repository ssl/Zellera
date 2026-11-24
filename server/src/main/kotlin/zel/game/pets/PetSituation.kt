package zel.game.pets

import java.util.Locale

enum class PetSituation {
    DISOBEY,
    DRINKING,
    EATING,
    GENERIC_HAPPY,
    GENERIC_NEUTRAL,
    GENERIC_SAD,
    GREET_OWNER,
    HUNGRY,
    LEVEL_UP,
    MUTED,
    PLAYFUL,
    PLAYING,
    REVIVED,
    SLEEPING,
    THIRSTY,
    TIRED,
    TREATED,
    UNKNOWN_COMMAND,
    ;

    companion object {
        private val byName = entries.associateBy { it.name }

        /** The situation a content row names, in any case; null for one the game never asks for. */
        fun named(text: String): PetSituation? = byName[text.uppercase(Locale.ROOT)]
    }
}
