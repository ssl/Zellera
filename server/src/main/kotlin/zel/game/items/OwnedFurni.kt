package zel.game.items

/** The owner it last wrote guards late saves after the furni changed hands. */
class OwnedFurni(
    val id: Int,
    userId: Int,
    val definition: Item,
    state: String,
    val editionSize: Int,
    val editionNumber: Int,
    val spot: FurniSpot = FurniSpot.NOWHERE,
    roomId: Int = 0,
    guildId: Int = 0,
) : Runnable {
    @Volatile var userId: Int = userId

    @Volatile var persistedUserId: Int = userId

    @Volatile var state: String = state

    @Volatile var roomId: Int = roomId

    @Volatile var guildId: Int = guildId

    @Volatile var changed: Boolean = false

    val dbId: Int get() = id

    val isLimited: Boolean get() = editionSize > 0 || editionNumber > 0

    /** The id the inventory strip uses: wall furni positive, floor furni negative. */
    val stripId: Int get() = if (definition.type == ProductKind.WALL_FURNI) id else -id

    /** 0 for anything but a music disc. */
    val songId: Int
        get() = if (definition.interaction.name == ItemInteraction.MUSIC_DISC) MusicDisc.songId(state) else 0

    val storedState: String get() = state.take(Items.MAX_STATE_LENGTH)

    fun markChanged() {
        changed = true
    }

    /** Clears [changed], so a change from here on is saved next time. */
    fun ownerToSave(): Int {
        changed = false
        return userId
    }

    fun markSaved(writtenOwner: Int) {
        persistedUserId = writtenOwner
    }

    override fun run() {
        OwnedFurniRows.save(definition.services.dataSource, this)
    }

    override fun toString(): String = "OwnedFurni($id ${definition.name} of $userId)"
}
