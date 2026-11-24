package zel.room.rights

import zel.protocol.generated.RoomChatSettingsOut
import zel.protocol.generated.RoomChatSettingsOutCodec
import zel.protocol.generated.RoomInfoUpdatedOut
import zel.protocol.generated.RoomInfoUpdatedOutCodec
import zel.protocol.generated.RoomSettingsDataOut
import zel.protocol.generated.RoomSettingsDataOutCodec
import zel.protocol.generated.RoomSettingsSaveErrorOut
import zel.protocol.generated.RoomSettingsSaveErrorOutCodec
import zel.protocol.generated.RoomSettingsSavedOut
import zel.protocol.generated.RoomSettingsSavedOutCodec
import zel.protocol.generated.RoomVisualizationSettingsOutCodec
import zel.room.RoomInfo
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.entry.DoorSecret
import zel.room.map.MapMessages
import zel.room.tick.Audience
import zel.room.tick.Lane

class SettingsEditor(
    private val registry: RoomRegistry,
    private val store: RightsStore,
    private val infos: (Int) -> RoomInfo?,
    private val staffTags: () -> Set<String>,
    private val settled: (RoomScope, RoomSettings) -> Unit = { _, _ -> },
) {
    fun get(
        playerId: Int,
        roomId: Int,
        mayEditAny: Boolean,
        reply: Reply,
    ) {
        val settings = store.settings(roomId) ?: return
        if (settings.ownerId != playerId && !mayEditAny) return
        reply.send(RoomSettingsDataOutCodec, data(roomId, settings))
    }

    fun save(
        playerId: Int,
        roomId: Int,
        wanted: RoomSettings,
        mayEditAny: Boolean,
        staff: Boolean,
        rank: Int,
        reply: Reply,
    ) {
        val current = store.settings(roomId) ?: return
        if (current.ownerId != playerId && !mayEditAny) return
        val tags = if (staff) emptySet() else staffTags()
        val category =
            wanted.categoryId == current.categoryId || store.categoryOpenTo(wanted.categoryId, rank, staff)
        when (val answer = SettingsCheck.check(wanted, current, tags, category)) {
            SettingsAnswer.Ignored -> {}

            is SettingsAnswer.Refused -> {
                val error = RoomSettingsSaveErrorOut(roomId, answer.code, answer.text)
                reply.send(RoomSettingsSaveErrorOutCodec, error)
            }

            is SettingsAnswer.Saved -> {
                store.saveSettings(roomId, sealed(answer.settings))
                applied(roomId)
                reply.send(RoomSettingsSavedOutCodec, RoomSettingsSavedOut(roomId))
            }
        }
    }

    /** A password door's typed password leaves here as its stored form; any other door keeps what is stored. */
    private fun sealed(settings: RoomSettings): RoomSettings =
        if (settings.doorMode == SettingsCheck.PASSWORD_DOOR) {
            settings.copy(password = DoorSecret.seal(settings.password))
        } else {
            settings
        }

    fun applied(roomId: Int) {
        if (registry.loaded(roomId) == null) return
        val info = infos(roomId) ?: return
        val settings = store.settings(roomId)
        registry.withRoom(roomId) { scope ->
            scope.room.updateInfo(info)
            settings?.let { settled(scope, it) }
            val out = scope.out
            val visuals = MapMessages.visualization(info.visuals)
            out.send(Lane.Now, Audience.Room, RoomVisualizationSettingsOutCodec, visuals)
            out.send(
                Lane.Now,
                Audience.Room,
                RoomChatSettingsOutCodec,
                RoomChatSettingsOut(info.moderation.floodFilter),
            )
            out.send(Lane.Now, Audience.Room, RoomInfoUpdatedOutCodec, RoomInfoUpdatedOut(roomId))
        }
    }

    private fun data(
        roomId: Int,
        s: RoomSettings,
    ) = RoomSettingsDataOut(
        int1 = roomId,
        string1 = s.name,
        string2 = s.description,
        int2 = s.doorMode,
        int3 = s.categoryId,
        int4 = s.maxUsers,
        int5 = s.maxUsers,
        list1 = s.tags,
        int6 = s.tradeMode,
        int7 = s.allowPets.int(),
        int8 = s.allowPetsEat.int(),
        int9 = s.allowWalkThrough.int(),
        int10 = s.hideWalls.int(),
        int11 = s.wallThickness,
        int12 = s.floorThickness,
        int13 = s.floodFilter,
        boolean1 = s.leaveOnDoorTile,
        boolean2 = s.idleSleep,
        int14 = s.idleSleepSeconds,
        boolean3 = s.idleKick,
        int15 = s.idleKickSeconds,
        boolean4 = s.muteAllPets,
        int16 = s.whoCanMute,
        int17 = s.whoCanKick,
        int18 = s.whoCanBan,
        boolean5 = false,
    )

    private fun Boolean.int(): Int = if (this) 1 else 0
}
