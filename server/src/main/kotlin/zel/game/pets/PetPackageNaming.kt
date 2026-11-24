package zel.game.pets

import org.apache.commons.lang3.StringUtils

object PetPackageNaming {
    const val CLOSE_WIDGET = 0
    const val NAME_TOO_LONG = 1
    const val NAME_TOO_SHORT = 2
    const val CONTAINS_INVALID_CHARS = 3

    fun validate(
        name: String?,
        minimum: Int,
        maximum: Int,
    ): Int =
        when {
            name == null || name.length < minimum -> NAME_TOO_SHORT
            name.length > maximum -> NAME_TOO_LONG
            !StringUtils.isAlphanumeric(name) -> CONTAINS_INVALID_CHARS
            else -> CLOSE_WIDGET
        }

    fun info(
        status: Int,
        name: String?,
        minimum: Int,
        maximum: Int,
    ): String =
        when (status) {
            NAME_TOO_SHORT -> minimum.toString()
            NAME_TOO_LONG -> maximum.toString()
            CONTAINS_INVALID_CHARS -> name?.firstOrNull { !it.isLetterOrDigit() }?.toString().orEmpty()
            else -> ""
        }
}
