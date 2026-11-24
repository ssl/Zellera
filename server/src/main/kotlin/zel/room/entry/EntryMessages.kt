package zel.room.entry

import zel.game.items.FurniHeights
import zel.protocol.generated.ConfigurationItemStatesOutCodec
import zel.protocol.generated.GetGuestRoomResultOut
import zel.protocol.generated.ItemsOut
import zel.protocol.generated.ObjectsOut
import zel.protocol.generated.OutgoingCodec
import zel.protocol.generated.PacketReader
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.RoomEventOut
import zel.protocol.generated.RoomPropertyOut
import zel.protocol.generated.RoomQueueStatusOut
import zel.protocol.generated.RoomUnitsOut
import zel.room.DoorMode
import zel.room.furni.BuildPass
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.special.SpecialStuff
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Interactions
import zel.room.unit.RoomUnit

object EntryMessages {
    private const val EVENT_TYPE = 1
    private const val PLAYER_ENTRY = 1
    private const val NO_GROUP = -1
    private const val NO_EXPIRY = -1
    private const val AT_REST = "0"
    private const val CONTRACT_CHEST = "wf_contract_"
    private const val RANKING = 2
    private const val SHOW_OWNER = 8
    private const val HAS_GROUP = 2
    private const val HAS_PROMOTION = 4
    private const val ALLOWS_PETS = 16
    private const val DEFAULT_PAPER = "0.0"
    private const val VISITORS_QUEUE = "visitors"
    private const val SPECTATORS_QUEUE = "spectators"
    private const val NO_EVENT = -1
    private const val DOOR_OPEN = 0
    private const val DOOR_BELL = 1
    private const val DOOR_PASSWORD = 2
    private const val DOOR_INVISIBLE = 3

    fun users(entries: List<Pair<PlayerCard, RoomUnit>>): RoomUnitsOut {
        val w = PacketWriter().int(entries.size)
        entries.forEach { (card, unit) -> writePlayer(w, card, unit) }
        return RoomUnitsOut(w.toByteArray())
    }

    private fun writePlayer(
        w: PacketWriter,
        card: PlayerCard,
        unit: RoomUnit,
    ) {
        w
            .int(card.id)
            .string(card.name)
            .string(card.motto)
            .string(card.figure)
            .int(unit.index)
            .int(unit.tile.x)
            .int(unit.tile.y)
            .string(FurniHeights.formatZ(unit.z))
            .int(unit.bodyDirection)
            .int(PLAYER_ENTRY)
            .string(card.gender)
            .int(card.group?.id ?: NO_GROUP)
            .int(card.group?.status ?: NO_GROUP)
            .string(card.group?.name.orEmpty())
            .string("")
            .int(card.achievementScore)
            .boolean(card.isModerator)
            .int(card.badgesRank)
    }

    fun doorCode(door: DoorMode): Int =
        when (door) {
            DoorMode.Open -> DOOR_OPEN
            DoorMode.Doorbell -> DOOR_BELL
            is DoorMode.Password -> DOOR_PASSWORD
            DoorMode.Invisible -> DOOR_INVISIBLE
        }

    data class RoomInfoView(
        val enterRoom: Boolean,
        val userCount: Int,
        val group: RoomGroup?,
        val promotion: RoomPromotion?,
        val isGroupMember: Boolean,
        val allMuted: Boolean,
        val canMute: Boolean,
        val forward: Boolean = false,
    )

    fun roomInfo(
        card: RoomCard,
        view: RoomInfoView,
    ): GetGuestRoomResultOut {
        val flags =
            SHOW_OWNER or
                (if (view.group != null) HAS_GROUP else 0) or
                (if (view.promotion != null) HAS_PROMOTION else 0) or
                (if (card.allowPets) ALLOWS_PETS else 0)
        val tail = PacketWriter()
        view.group?.let { tail.int(it.id).string(it.name).string(it.badge) }
        view.promotion?.let { tail.string(it.title).string(it.description).int(it.minutesLeft) }
        tail
            .boolean(view.forward)
            .boolean(card.staffPicked)
            .boolean(view.isGroupMember)
            .boolean(view.allMuted)
            .int(card.whoCanMute)
            .int(card.whoCanKick)
            .int(card.whoCanBan)
            .boolean(view.canMute)
            .int(card.chatProtection)
            .boolean(false)
        return GetGuestRoomResultOut(
            view.enterRoom,
            card.id,
            card.name,
            card.ownerId,
            card.ownerName,
            doorCode(card.door),
            view.userCount,
            card.maxUsers,
            card.description,
            card.tradeMode,
            card.score,
            RANKING,
            card.categoryId,
            card.tags,
            flags,
            tail.toByteArray(),
        )
    }

    /** RoomProperty messages: wallpaper and floor when set, the landscape always. */
    fun properties(card: RoomCard): List<RoomPropertyOut> =
        buildList {
            if (card.wallpaper.isSet()) add(RoomPropertyOut("wallpaper", card.wallpaper))
            if (card.floor.isSet()) add(RoomPropertyOut("floor", card.floor))
            add(RoomPropertyOut("landscape", card.landscape.ifEmpty { DEFAULT_PAPER }))
        }

    private fun String.isSet(): Boolean = isNotEmpty() && this != DEFAULT_PAPER

    fun event(
        card: RoomCard,
        promotion: RoomPromotion,
    ): RoomEventOut =
        RoomEventOut(
            card.id,
            card.ownerId,
            card.ownerName,
            card.id,
            EVENT_TYPE,
            promotion.title,
            promotion.description,
            0,
            promotion.minutesLeft,
            promotion.category,
        )

    fun noEvent(): RoomEventOut = RoomEventOut(NO_EVENT, NO_EVENT, "", 0, 0, "", "", 0, 0, 0)

    private fun owners(furni: Collection<RoomFurni>): List<Pair<Int, String>> =
        furni
            .flatMap { listOf(it.ownerId to it.ownerName, it.placerId to it.placerName) }
            .distinctBy { it.first }
            .sortedWith(compareBy({ it.first != BuildPass.OWNER_ID }, { it.first }))

    /**
     * [boxesAtRest]: wired boxes go out as state 0 whatever their own state, as an area hide's give-back does (lamps
     * keep their own state); their stored state stays.
     */
    fun objects(
        furni: Collection<RoomFurni>,
        boxesAtRest: Boolean = false,
    ): ObjectsOut =
        ObjectsOut(
            owners(furni).map { (id, name) -> ObjectsOut.List1Item(id, name) },
            furni.filter { !it.isWall && it.anchor != null }.map { floorItem(it, boxesAtRest) },
        )

    private fun floorItem(
        furni: RoomFurni,
        boxesAtRest: Boolean,
    ): ObjectsOut.List2Item {
        val add = FurniWire.objectAdd(furni)
        val type = Interactions.of(furni.definition)
        // A contract chest is no blinking box: it keeps the plain furni state it always sends.
        val atRest = boxesAtRest && Interactions.isWiredBox(type) && !type.startsWith(CONTRACT_CHEST)
        return ObjectsOut.List2Item(
            add.int1,
            add.int2,
            add.int3,
            add.int4,
            add.int5,
            add.string1,
            add.string2,
            add.int6,
            if (atRest) SpecialStuff.legacy(furni, AT_REST) else add.stuff1,
            add.int7,
            add.int8,
            add.int9,
            ObjectsOut.List2Item.Branch1.Else,
        )
    }

    fun items(furni: Collection<RoomFurni>): ItemsOut =
        ItemsOut(
            owners(furni).map { (id, name) -> ItemsOut.List1Item(id, name) },
            furni.filter { it.isWall }.map { wall ->
                val add = FurniWire.itemAdd(wall)
                ItemsOut.List2Item(
                    add.string1,
                    add.int1,
                    add.string2,
                    add.string3,
                    NO_EXPIRY,
                    add.int3,
                    add.int4,
                )
            },
        )

    /** Both queues, the player's own first as the active one, each with one entry: how many are ahead. */
    fun queueStatus(
        roomId: Int,
        spectator: Boolean,
        ahead: Int,
        aheadInOther: Int = 0,
    ): RoomQueueStatusOut {
        val visitors = queueItem(VISITORS_QUEUE, EntryQueue.VISITOR, if (spectator) aheadInOther else ahead)
        val spectators = queueItem(SPECTATORS_QUEUE, EntryQueue.SPECTATOR, if (spectator) ahead else aheadInOther)
        return RoomQueueStatusOut(roomId, if (spectator) listOf(spectators, visitors) else listOf(visitors, spectators))
    }

    private fun queueItem(
        name: String,
        target: Int,
        ahead: Int,
    ) = RoomQueueStatusOut.List1Item(name, target, listOf(RoomQueueStatusOut.List1Item.List1Item(name, ahead)))

    data class ConfigurationStates(
        val states: List<Boolean>,
    )

    object ConfigurationStatesCodec : OutgoingCodec<ConfigurationStates> {
        override val name = ConfigurationItemStatesOutCodec.name

        override fun read(r: PacketReader): ConfigurationStates = ConfigurationStates(r.untilEnd { r.boolean() })

        override fun write(
            w: PacketWriter,
            m: ConfigurationStates,
        ) {
            m.states.forEach { w.boolean(it) }
        }
    }
}
