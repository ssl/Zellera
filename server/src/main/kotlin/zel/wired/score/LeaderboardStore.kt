package zel.wired.score

interface LeaderboardStore {
    fun holders(
        roomId: Int,
        boxId: Int,
    ): List<LeaderHolder>

    fun marks(roomId: Int): Map<Int, Pair<Int, Int>>

    fun changed() = Unit

    fun saveMark(
        boxId: Int,
        publishedId: Int,
        revision: Int,
    )

    companion object {
        const val PUBLISHED_KEY = "publishedId"
        const val REVISION_KEY = "revision"
    }
}
