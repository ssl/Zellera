package zel.game.players

class FavouriteRooms internal constructor(
    private val store: PlayerStatsStore,
    private val limit: () -> Int,
) {
    private val ids = ArrayList<Int>()

    val all: List<Int> get() = synchronized(ids) { ids.toList() }

    val count: Int get() = synchronized(ids) { ids.size }

    fun add(roomId: Int): Boolean {
        synchronized(ids) { if (roomId in ids || ids.size >= limit()) return false }
        store.addFavouriteRoom(roomId)
        synchronized(ids) { ids.add(roomId) }
        return true
    }

    fun remove(roomId: Int) {
        if (synchronized(ids) { ids.remove(roomId) }) store.removeFavouriteRoom(roomId)
    }

    internal fun restore(roomId: Int) {
        synchronized(ids) { ids.add(roomId) }
    }
}
