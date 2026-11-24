package zel.room.furni.place

interface FurniInventory {
    fun find(
        playerId: Int,
        itemId: Int,
    ): InventoryItem?

    fun take(
        playerId: Int,
        itemId: Int,
    ): Boolean

    /** Runs on the persistence executor, outside every room lock. */
    fun handBack(
        ownerId: Int,
        furniId: Int,
    )

    fun handBackAll(
        ownerId: Int,
        furniIds: List<Int>,
    ) = furniIds.forEach { handBack(ownerId, it) }

    fun handBackQuietly(
        ownerId: Int,
        furniIds: List<Int>,
    ) = handBackAll(ownerId, furniIds)

    fun refresh(ownerId: Int) = Unit
}
