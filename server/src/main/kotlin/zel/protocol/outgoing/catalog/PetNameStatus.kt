@file:Suppress("MagicNumber") // the code numbers the client reads

package zel.protocol.outgoing.catalog

enum class PetNameStatus(
    val code: Int,
) {
    OK(0),
    TOO_LONG(1),
    TOO_SHORT(2),
    BAD_CHARACTER(3),
    BAD_WORD(4),
}
