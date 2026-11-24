package zel.room.entry

import zel.protocol.generated.DoorbellOut
import zel.protocol.generated.DoorbellOutCodec
import zel.protocol.generated.FlatAccessibleOut
import zel.protocol.generated.FlatAccessibleOutCodec
import zel.protocol.generated.FloorHeightMapOutCodec
import zel.protocol.generated.FurnitureLabelsAndRegionsOutCodec
import zel.protocol.generated.GetGuestRoomResultOutCodec
import zel.protocol.generated.GroupBadgesOut
import zel.protocol.generated.GroupBadgesOutCodec
import zel.protocol.generated.HeightMapOutCodec
import zel.protocol.generated.ItemsOutCodec
import zel.protocol.generated.NotificationDialogOutCodec
import zel.protocol.generated.ObjectsOutCodec
import zel.protocol.generated.OpenConnectionOut
import zel.protocol.generated.OpenConnectionOutCodec
import zel.protocol.generated.OutgoingCodec
import zel.protocol.generated.PlayerEffectOut
import zel.protocol.generated.PlayerEffectOutCodec
import zel.protocol.generated.RaidProtectionCapabilityOut
import zel.protocol.generated.RaidProtectionCapabilityOutCodec
import zel.protocol.generated.RoomEntryInfoOut
import zel.protocol.generated.RoomEntryInfoOutCodec
import zel.protocol.generated.RoomEntryTileOutCodec
import zel.protocol.generated.RoomEventOutCodec
import zel.protocol.generated.RoomPropertyOutCodec
import zel.protocol.generated.RoomRatingOut
import zel.protocol.generated.RoomRatingOutCodec
import zel.protocol.generated.RoomReadyOut
import zel.protocol.generated.RoomReadyOutCodec
import zel.protocol.generated.RoomUnitUpdateOut
import zel.protocol.generated.RoomUnitUpdateOutCodec
import zel.protocol.generated.RoomUnitsOutCodec
import zel.protocol.generated.RoomVisualizationSettingsOutCodec
import zel.protocol.generated.WiredEnvironmentOutCodec
import zel.protocol.generated.WiredPermissionsOut
import zel.protocol.generated.WiredPermissionsOutCodec
import zel.protocol.generated.YouAreControllerOut
import zel.protocol.generated.YouAreControllerOutCodec
import zel.protocol.generated.YouAreOwnerOut
import zel.protocol.generated.YouAreOwnerOutCodec
import zel.protocol.generated.YouAreSpectatorOut
import zel.protocol.generated.YouAreSpectatorOutCodec
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomHeights
import zel.room.furni.Steppable
import zel.room.furni.transit.TileTeleport
import zel.room.map.MapMessages
import zel.room.map.Point
import zel.room.rights.ModerationSetting
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.UnitStatus
import zel.room.unit.path.WalkTiles
import zel.room.unit.walk.UnitLines
import zel.room.unit.walk.Walking

class EntryBurst(
    private val registry: RoomRegistry,
    private val walking: Walking,
    private val leaving: RoomLeaving,
    private val views: EntryViews = EntryViews.None,
    private val spots: ArrivalSpots = ArrivalSpots.None,
) {
    fun enter(
        scope: RoomScope,
        door: RoomDoor,
        pending: Pending,
        arrivalFurni: Int?,
        afterBell: Boolean = false,
        arrivalEffect: Int = 0,
        entryMethod: Int = RoomEntryMethod.DEFAULT,
    ) {
        if (!door.initialized) {
            door.initialized = true
            scope.raise(RoomEvent.Initialized)
        }
        val playerId = pending.card.id
        val level = pending.visitor.level
        val unit = place(scope, playerId, level, arrivalFurni)
        unit.entryMethod = if (entryMethod == RoomEntryMethod.TELEPORTER) RoomEntryMethod.TELEPORT else entryMethod
        if (entryMethod == RoomEntryMethod.TELEPORTER) unit.entryTeleportId = arrivalFurni ?: 0
        door.present[playerId] = Present(unit.index, level, pending.card, leaving.sessionOf(playerId))
        registry.enter(playerId, scope.room.info.id)
        sendRoom(scope, pending, afterBell, own = unit)
        sendUnits(scope, door, unit, playerId)
        sendRights(scope, pending, level)
        leaving.ownerStatus(scope)
        if (level >= DoorCheck.RIGHTS) {
            door.ringing.values.forEach { ringing ->
                to(scope, playerId, DoorbellOutCodec, DoorbellOut(ringing.card.name))
            }
        }
        views.afterBurst(scope, unit, playerId)
        if (arrivalFurni != null && unit.tile == scope.room.furni[arrivalFurni]?.anchor) {
            views.arrivedThrough(scope, unit, arrivalFurni)
        }
        stepOnStart(scope, unit, arrivalFurni)
        scope.raise(RoomEvent.Entered(unit.index))
        // A linker's id is readable only after the enter stacks ran.
        if (entryMethod == RoomEntryMethod.TELEPORT) unit.entryTeleportId = arrivalFurni ?: 0
        if (arrivalEffect != 0) arrivalEffect(scope, unit, arrivalEffect)
    }

    fun enterAsSpectator(
        scope: RoomScope,
        door: RoomDoor,
        pending: Pending,
    ) {
        if (!door.initialized) {
            door.initialized = true
            scope.raise(RoomEvent.Initialized)
        }
        val playerId = pending.card.id
        val level = pending.visitor.level
        door.present[playerId] =
            Present(Present.NO_UNIT, level, pending.card, leaving.sessionOf(playerId), spectator = true)
        scope.room.spectators += playerId
        registry.enter(playerId, scope.room.info.id)
        sendRoom(scope, pending, afterBell = false, spectator = true)
        sendView(scope, door, playerId)
        sendRights(scope, pending, level)
        leaving.ownerStatus(scope)
    }

    fun promote(
        scope: RoomScope,
        door: RoomDoor,
        playerId: Int,
    ) {
        val watching = door.present[playerId]?.takeIf { it.spectator } ?: return
        scope.room.spectators -= playerId
        val unit = place(scope, playerId, watching.level, arrivalFurni = null)
        door.present[playerId] = watching.copy(unitIndex = unit.index, spectator = false)
        sendOwnUnit(scope, unit, playerId)
        views.afterBurst(scope, unit, playerId)
        stepOnStart(scope, unit, arrivalFurni = null)
        scope.raise(RoomEvent.Entered(unit.index))
    }

    /**
     * Appearing on the start tile counts as stepping onto its top item: walks-on fires and the item's step
     * behaviour runs, before Entered (a doormat on the door tile fires its walks-on stack in each entry burst). The
     * arrival furni's own transit already handles the arrival, so its step behaviour is not run again (a teleport
     * tile would start a new ride).
     */
    private fun stepOnStart(
        scope: RoomScope,
        unit: RoomUnit,
        arrivalFurni: Int?,
    ) {
        val top =
            scope.room.furni
                .at(unit.tile)
                .stepItem() ?: return
        // Starting on a teleport tile does not ride it; only walking onto it does.
        if (top.id != arrivalFurni && top.behaviour !is TileTeleport) {
            (top.behaviour as? Steppable)?.onStepOn(scope, top, unit)
        }
        scope.raise(RoomEvent.StepOn(unit.index, top.id))
    }

    private fun place(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        arrivalFurni: Int?,
    ): RoomUnit {
        val room = scope.room
        val spot = spots.spotOf(scope, playerId)?.takeIf { room.floor.isFloor(it.tile) }
        val arrival = arrivalFurni?.let { room.furni[it] }?.takeIf { it.anchor != null && spot == null }
        val tile: Point = spot?.tile ?: arrival?.anchor ?: room.floor.door.tile
        val direction = spot?.direction ?: arrival?.direction ?: room.floor.door.direction
        val unit = RoomUnit(room.units.nextIndex(), UnitKind.Player(playerId), tile, 0.0, direction, direction)
        if (level >= DoorCheck.RIGHTS) unit.setStatus(UnitStatus.FLAT_CONTROL, level.toString())
        room.units.add(unit)
        val z =
            when {
                spot != null -> spot.z
                arrival == null -> WalkTiles.entryHeight(room, tile)
                else -> null
            }
        walking.place(scope, unit, tile, z)
        if (arrival != null) walking.arrivedByTransit(scope, unit)
        return unit
    }

    private fun sendRoom(
        scope: RoomScope,
        pending: Pending,
        afterBell: Boolean,
        spectator: Boolean = false,
        own: RoomUnit? = null,
    ) {
        val room = scope.room
        val card = pending.room
        val playerId = pending.card.id
        val roomId = room.info.id
        val level = pending.visitor.level
        val facts = pending.facts
        val view =
            EntryMessages.RoomInfoView(
                enterRoom = true,
                userCount = room.units.playerIds().size,
                group = facts.group,
                promotion = facts.promotion,
                isGroupMember = facts.isGroupMember,
                allMuted = views.allMuted(scope),
                canMute = ModerationSetting.allows(card.whoCanMute, level, card.groupId),
            )
        to(scope, playerId, GetGuestRoomResultOutCodec, EntryMessages.roomInfo(card, view))
        if (!afterBell) {
            to(scope, playerId, OpenConnectionOutCodec, OpenConnectionOut(roomId))
            to(scope, playerId, FlatAccessibleOutCodec, FlatAccessibleOut(roomId, ""))
        }
        if (spectator) to(scope, playerId, YouAreSpectatorOutCodec, YouAreSpectatorOut(roomId))
        to(scope, playerId, RoomReadyOutCodec, RoomReadyOut(room.info.modelName, roomId))
        // the room's door, right after RoomReady; without it the client picks a door from the map's shape and
        // raises that tile to wall height
        to(scope, playerId, RoomEntryTileOutCodec, MapMessages.entryTile(room.floor.door))
        EntryMessages.properties(card).forEach { to(scope, playerId, RoomPropertyOutCodec, it) }
        val hidden = views.hiddenTiles(scope)
        val heights =
            MapMessages.heightMap(room.floor) { tile ->
                if (tile in hidden) MapMessages.VOID_VALUE else RoomHeights.value(room, tile)
            }
        to(scope, playerId, HeightMapOutCodec, heights)
        val floorMap =
            if (own == null) {
                MapMessages.floorHeightMap(room.floor, views.areaHides(scope))
            } else {
                MapMessages.floorHeightMap(room.floor, views.areaHides(scope), own.tile, own.z.toFloat())
            }
        to(scope, playerId, FloorHeightMapOutCodec, floorMap)
        to(scope, playerId, RoomVisualizationSettingsOutCodec, MapMessages.visualization(room.info.visuals))
    }

    private fun sendUnits(
        scope: RoomScope,
        door: RoomDoor,
        unit: RoomUnit,
        playerId: Int,
    ) {
        val card = requireNotNull(door.present[playerId]).card
        scope.out.send(Lane.Now, Audience.Room, RoomUnitsOutCodec, EntryMessages.users(listOf(card to unit)))
        sendView(scope, door, playerId, unit)
    }

    private fun sendView(
        scope: RoomScope,
        door: RoomDoor,
        playerId: Int,
        own: RoomUnit? = null,
    ) {
        val room = scope.room
        val everyone =
            door.present.values.mapNotNull { present -> room.units[present.unitIndex]?.let { present.card to it } }
        to(scope, playerId, RoomUnitsOutCodec, EntryMessages.users(everyone))
        if (!views.otherUnits(scope, playerId)) {
            to(scope, playerId, RoomUnitsOutCodec, EntryMessages.users(emptyList()))
        }
        if (own != null) {
            val line = UnitLines.line(own, null)
            scope.out.send(Lane.Now, Audience.Room, RoomUnitUpdateOutCodec, RoomUnitUpdateOut(listOf(line)))
        }
        val lines = room.units.all().map { UnitLines.line(it, null) }
        to(scope, playerId, RoomUnitUpdateOutCodec, RoomUnitUpdateOut(lines))
        if (own != null) worn(scope, own)
        room.sight.forget(playerId)
        to(scope, playerId, ObjectsOutCodec, EntryMessages.objects(views.shownFurni(scope, room.furni.all())))
        to(scope, playerId, ItemsOutCodec, EntryMessages.items(views.shownFurni(scope, room.furni.all())))
        views.afterFurni(scope, playerId)
    }

    private fun sendOwnUnit(
        scope: RoomScope,
        unit: RoomUnit,
        playerId: Int,
    ) {
        val card = requireNotNull(leaving.doorOf(scope.room).present[playerId]).card
        scope.out.send(Lane.Now, Audience.Room, RoomUnitsOutCodec, EntryMessages.users(listOf(card to unit)))
        val line = UnitLines.line(unit, null)
        scope.out.send(Lane.Now, Audience.Room, RoomUnitUpdateOutCodec, RoomUnitUpdateOut(listOf(line)))
        worn(scope, unit)
    }

    /**
     * A room trip's effect on the arriving user at the end of the entry burst: AvatarEffect(user, 234) after
     * RaidProtectionCapability and the room's own entry chat, then AvatarEffect(user, 0), both sent at once.
     */
    private fun arrivalEffect(
        scope: RoomScope,
        unit: RoomUnit,
        effect: Int,
    ) {
        if (scope.room.units[unit.index] !== unit) return
        scope.out.send(Lane.Now, Audience.Room, PlayerEffectOutCodec, PlayerEffectOut(unit.index, effect, 0))
        val room = scope.room
        registry.post(room.info.id, ARRIVAL_EFFECT_MILLIS) { later ->
            if (later.room !== room || room.units[unit.index] !== unit) return@post
            val own = PlayerEffectOut(unit.index, views.effect(unit), 0)
            later.out.send(Lane.Now, Audience.Room, PlayerEffectOutCodec, own)
        }
    }

    /** The entrant's own effect goes to the room only when it wears one. */
    private fun worn(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        val effect = views.effect(unit)
        if (effect == 0) return
        scope.out.send(Lane.Now, Audience.Room, PlayerEffectOutCodec, PlayerEffectOut(unit.index, effect, 0))
    }

    private fun sendRights(
        scope: RoomScope,
        pending: Pending,
        level: Int,
    ) {
        val playerId = pending.card.id
        val roomId = scope.room.info.id
        val owner = level >= DoorCheck.OWNER
        to(scope, playerId, RoomEntryInfoOutCodec, RoomEntryInfoOut(roomId, owner))
        views.entryNotices(scope, level).forEach { to(scope, playerId, NotificationDialogOutCodec, it) }
        to(
            scope,
            playerId,
            RoomEventOutCodec,
            pending.facts.promotion?.let { EntryMessages.event(pending.room, it) } ?: EntryMessages.noEvent(),
        )
        val states = EntryMessages.ConfigurationStates(views.configurationStates(scope, playerId))
        to(scope, playerId, EntryMessages.ConfigurationStatesCodec, states)
        to(scope, playerId, WiredEnvironmentOutCodec, views.wiredEnvironment(scope))
        to(scope, playerId, FurnitureLabelsAndRegionsOutCodec, views.labels(scope, level))
        to(scope, playerId, YouAreControllerOutCodec, YouAreControllerOut(roomId, level))
        val (modify, read) = views.wiredPermissions(scope, level)
        to(scope, playerId, WiredPermissionsOutCodec, WiredPermissionsOut(modify, read))
        if (owner) {
            to(scope, playerId, YouAreOwnerOutCodec, YouAreOwnerOut(roomId))
        }
        val canRate = !owner && !pending.facts.voted
        to(scope, playerId, RoomRatingOutCodec, RoomRatingOut(pending.room.score, canRate))
        to(
            scope,
            playerId,
            RaidProtectionCapabilityOutCodec,
            RaidProtectionCapabilityOut(
                roomId,
                level >= DoorCheck.RIGHTS,
            ),
        )
        val badges =
            leaving
                .doorOf(scope.room)
                .present.values
                .mapNotNull { it.card.group }
                .distinctBy { it.id }
                .map { GroupBadgesOut.List1Item(it.id, it.badge) }
        to(scope, playerId, GroupBadgesOutCodec, GroupBadgesOut(badges))
    }

    private fun <T> to(
        scope: RoomScope,
        playerId: Int,
        codec: OutgoingCodec<T>,
        message: T,
    ) = scope.out.send(Lane.Now, Audience.Player(playerId), codec, message)

    companion object {
        /** From the arrival's effect to the user's own effect again. */
        const val ARRIVAL_EFFECT_MILLIS = 1000L
    }
}
