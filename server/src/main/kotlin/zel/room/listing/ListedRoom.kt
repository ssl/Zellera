package zel.room.listing

import zel.protocol.BodyPart
import zel.protocol.generated.PacketWriter

class ListedRoom(
    val id: Int,
    private val settings: ListedSettings,
    val group: ListedGroup? = null,
    val promotion: ListedPromotion? = null,
    val userCount: Int = 0,
    val isLoaded: Boolean = false,
) : BodyPart,
    Comparable<ListedRoom> {
    val ownerId: Int get() = settings.ownerId
    val ownerName: String get() = settings.ownerName
    val name: String get() = settings.name
    val description: String get() = settings.description
    val doorCode: Int get() = settings.doorCode
    val maxPlayers: Int get() = settings.maxPlayers
    val category: Int get() = settings.category
    val score: Int get() = settings.score
    val isPublicRoom: Boolean get() = settings.isPublicRoom
    val isStaffPicked: Boolean get() = settings.isStaffPicked

    val guildId: Int get() = group?.id ?: 0
    val guildName: String get() = group?.name.orEmpty()
    val promotionDescription: String get() = promotion?.description.orEmpty()

    val tags: Array<String> get() = tagList.toTypedArray()

    val tagList: List<String> get() = settings.tagText.split(";").filter { it.isNotEmpty() }

    fun inGuild(): Boolean = guildId > 0

    fun filterAnything(): String =
        listOf(ownerName, name, settings.tagText, guildName)
            .joinToString(" ")

    override fun writeTo(body: PacketWriter) {
        body.int(id)
        body.string(name)
        body.int(if (isPublicRoom) 0 else ownerId)
        body.string(if (isPublicRoom) "" else ownerName)
        body.int(doorCode)
        body.int(userCount)
        body.int(maxPlayers)
        body.string(description)
        body.int(0)
        body.int(score)
        body.int(0)
        body.int(category)
        body.int(tagList.size)
        tagList.forEach { body.string(it) }
        body.int(flags())
        group?.let {
            body.int(it.id)
            body.string(it.name)
            body.string(it.badge)
        }
        promotion?.let {
            body.string(it.title)
            body.string(it.description)
            body.int(it.minutesLeft)
        }
    }

    private fun flags(): Int =
        (if (group != null) FLAG_GROUP else 0) or
            (if (promotion != null) FLAG_PROMOTION else 0) or
            (if (isPublicRoom) 0 else FLAG_PRIVATE) or
            (if (settings.allowsOtherPets) FLAG_PETS else 0)

    override fun compareTo(other: ListedRoom): Int =
        when {
            userCount != other.userCount -> other.userCount - userCount
            else -> id - other.id
        }

    override fun equals(other: Any?): Boolean = other is ListedRoom && other.id == id

    override fun hashCode(): Int = id

    companion object {
        private const val FLAG_GROUP = 2
        private const val FLAG_PROMOTION = 4
        private const val FLAG_PRIVATE = 8
        private const val FLAG_PETS = 16

        val BUSIEST_THEN_BY_NAME: Comparator<ListedRoom> =
            compareByDescending<ListedRoom> { it.userCount }
                .thenBy { it.name.lowercase() }
                .thenBy { it.id }

        val BUSIEST_THEN_TOP_SCORED: Comparator<ListedRoom> =
            compareByDescending<ListedRoom> { it.userCount }.thenByDescending { it.score }
    }
}
