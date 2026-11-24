package zel.room.entry

class EntryQueue {
    private val waiting = ArrayList<Queued>()

    fun isEmpty(): Boolean = waiting.isEmpty()

    fun add(queued: Queued) {
        remove(queued.playerId)
        waiting += queued
    }

    fun remove(playerId: Int): Queued? {
        val at = waiting.indexOfFirst { it.playerId == playerId }
        return if (at < 0) null else waiting.removeAt(at)
    }

    fun change(
        playerId: Int,
        target: Int,
    ): Boolean {
        val queued = remove(playerId) ?: return false
        waiting += queued.copy(spectator = target == SPECTATOR)
        return true
    }

    fun ahead(playerId: Int): Int {
        val me = waiting.firstOrNull { it.playerId == playerId } ?: return 0
        return waiting.takeWhile { it !== me }.count { it.spectator == me.spectator }
    }

    fun all(): List<Queued> = waiting.toList()

    companion object {
        const val SPECTATOR = 1
        const val VISITOR = 2
    }
}
