package zel.game.catalog.layouts

internal fun String.splitParts(delimiter: String): List<String> =
    if (delimiter in this) split(delimiter).dropLastWhile { it.isEmpty() } else listOf(this)
