package zel.game.players

import java.util.concurrent.ConcurrentHashMap

/** Changed on the owner's packet lane, read from room threads while they hear chat: the sets are concurrent. */
class PlayerContacts internal constructor(
    private val ownerId: Int,
    private val store: PlayerStatsStore,
) {
    private val ignored: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    private val blocked: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    val customFilter = CustomFilter()

    val ignoredIds: List<Int> get() = ignored.toTypedArray().sorted()

    val blockedIds: List<Int> get() = blocked.toTypedArray().sorted()

    fun userIgnored(userId: Int): Boolean = userId in ignored

    /** False when the list is full. */
    @Synchronized
    fun ignore(userId: Int): Boolean {
        if (userIgnored(userId)) return true
        if (ignored.size >= MAX_IGNORED) return false
        ignored.add(userId)
        store.addIgnore(userId)
        return true
    }

    @Synchronized
    fun unignore(userId: Int) {
        if (!ignored.remove(userId)) return
        store.removeIgnore(userId)
    }

    fun userBlocked(userId: Int): Boolean = userId in blocked

    /** False for oneself, nobody, a block already there, or a full list. */
    @Synchronized
    fun block(userId: Int): Boolean {
        if (userId <= 0 || userId == ownerId || userBlocked(userId)) return false
        if (blocked.size >= MAX_BLOCKED) return false
        blocked.add(userId)
        store.addBlock(userId)
        return true
    }

    @Synchronized
    fun unblock(userId: Int): Boolean {
        if (!blocked.remove(userId)) return false
        store.removeBlock(userId)
        return true
    }

    /** False for an empty word, one already there, or a full filter. */
    @Synchronized
    fun addFilterWord(word: String?): Boolean {
        if (customFilter.size() >= MAX_FILTER_WORDS || !customFilter.add(word)) return false
        store.addFilterWord(CustomFilter.normalize(word))
        return true
    }

    @Synchronized
    fun removeFilterWord(word: String?): Boolean {
        if (!customFilter.remove(word)) return false
        store.removeFilterWord(CustomFilter.normalize(word))
        return true
    }

    internal fun restoreIgnored(userId: Int) {
        ignored.add(userId)
    }

    internal fun restoreBlocked(userId: Int) {
        blocked.add(userId)
    }

    companion object {
        /** The whole list goes to the client at every login and is asked for every chat line heard. */
        const val MAX_IGNORED = 100

        /** Like [MAX_IGNORED]: the whole list goes to the client when it asks and is asked for every line heard. */
        const val MAX_BLOCKED = 100

        /** Every line the owner hears is run through all of them, on the speaker's lane. */
        const val MAX_FILTER_WORDS = 100
    }
}
