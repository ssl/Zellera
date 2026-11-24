package zel.room.chat

import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import java.util.concurrent.ConcurrentHashMap

class RoomWords(
    private val store: RoomWordStore,
    private val persistInOrder: (key: Any, job: Runnable) -> Unit,
    private val replacement: () -> String,
    private val maxWords: () -> Int,
) : RoomPart {
    private class Listed(
        val words: LinkedHashSet<String>,
    ) {
        var pattern: Regex? = compile()

        fun compile(): Regex? =
            words
                .filter { it.isNotEmpty() }
                .takeIf { it.isNotEmpty() }
                ?.joinToString("|") { Regex.escape(it) }
                ?.let { Regex(it, RegexOption.IGNORE_CASE) }
    }

    private val rooms = ConcurrentHashMap<Room, Listed>()

    override fun load(room: Room) {
        rooms[room] = Listed(LinkedHashSet(store.load(room.info.id)))
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
    }

    fun words(room: Room): List<String> = rooms[room]?.words?.toList().orEmpty()

    fun change(
        room: Room,
        add: Boolean,
        word: String,
    ) {
        if (word.length > MAX_LENGTH) return
        val listed = rooms.getOrPut(room) { Listed(LinkedHashSet()) }
        val roomId = room.info.id
        val changed =
            when {
                add -> listed.words.size < maxWords() && listed.words.add(word)
                else -> listed.words.remove(word)
            }
        if (!changed) return
        listed.pattern = listed.compile()
        persistInOrder(Key(roomId)) { if (add) store.add(roomId, word) else store.remove(roomId, word) }
    }

    fun apply(
        room: Room,
        text: String,
    ): String {
        val pattern = rooms[room]?.pattern ?: return text
        return pattern.replace(text, Regex.escapeReplacement(replacement()))
    }

    /** One write lane per room, so an add and a later removal of a word reach the database in that order. */
    private data class Key(
        val roomId: Int,
    )

    companion object {
        const val MAX_LENGTH = 255
    }
}
