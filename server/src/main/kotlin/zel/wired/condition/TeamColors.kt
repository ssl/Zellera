package zel.wired.condition

import zel.room.games.TeamColor

internal object TeamColors {
    fun of(value: Int): TeamColor? = TeamColor.fromCode(if (value == 0) TeamColor.RED.code else value)
}
