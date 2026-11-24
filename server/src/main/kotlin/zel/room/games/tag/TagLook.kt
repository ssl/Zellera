package zel.room.games.tag

data class TagLook(
    val male: Int,
    val female: Int,
) {
    fun effectFor(isMale: Boolean): Int = if (isMale) male else female
}
