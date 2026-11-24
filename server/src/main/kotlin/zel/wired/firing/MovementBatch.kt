package zel.wired.firing

/** One entry per moved object, in the order the objects first moved. */
class MovementBatch {
    private val entries = LinkedHashMap<Movement.Key, Movement>()

    fun add(movement: Movement) {
        entries[movement.key] = entries[movement.key]?.then(movement) ?: movement
    }

    /** Only the last move of the object's chain goes out. */
    fun replace(movement: Movement) {
        entries[movement.key] = movement
    }

    fun isEmpty(): Boolean = entries.isEmpty()

    fun entries(): List<Movement> = entries.values.toList()
}
