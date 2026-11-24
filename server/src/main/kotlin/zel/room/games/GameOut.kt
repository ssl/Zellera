package zel.room.games

import zel.protocol.generated.ExpressionOut
import zel.protocol.generated.ExpressionOutCodec
import zel.protocol.generated.GamePlayerValueOut
import zel.protocol.generated.GamePlayerValueOutCodec
import zel.protocol.generated.HeightMapUpdateOutCodec
import zel.protocol.generated.ObjectsDataUpdateOut
import zel.protocol.generated.ObjectsDataUpdateOutCodec
import zel.protocol.generated.PlayerEffectOut
import zel.protocol.generated.PlayerEffectOutCodec
import zel.protocol.generated.YouArePlayingGameOut
import zel.protocol.generated.YouArePlayingGameOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.RoomHeights
import zel.room.furni.place.FurniWire
import zel.room.map.MapMessages
import zel.room.map.Point
import zel.room.map.TileValue
import zel.room.tick.Audience
import zel.room.tick.Lane

object GameOut {
    private const val WAVE = 1

    fun states(
        scope: RoomScope,
        furni: Collection<RoomFurni>,
    ) {
        if (furni.isEmpty()) return
        val items =
            furni.distinctBy { it.id }.map {
                ObjectsDataUpdateOut.List1Item(
                    it.id,
                    FurniWire.stuff(it),
                )
            }
        scope.out.send(
            Lane.Now,
            Audience.Room,
            ObjectsDataUpdateOutCodec,
            ObjectsDataUpdateOut(items),
        )
    }

    fun playing(
        scope: RoomScope,
        playerId: Int,
        playing: Boolean,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        YouArePlayingGameOutCodec,
        YouArePlayingGameOut(playing),
    )

    fun playerValue(
        scope: RoomScope,
        member: TeamMember,
        value: Int,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(member.playerId),
        GamePlayerValueOutCodec,
        GamePlayerValueOut(member.unit.index, value),
    )

    fun wave(
        scope: RoomScope,
        unitIndex: Int,
    ) = scope.out.send(
        Lane.Now,
        Audience.Room,
        ExpressionOutCodec,
        ExpressionOut(unitIndex, WAVE),
    )

    fun delayedEffect(
        scope: RoomScope,
        unitIndex: Int,
        effect: Int,
        delay: Int,
    ) = scope.out.send(
        Lane.Now,
        Audience.Room,
        PlayerEffectOutCodec,
        PlayerEffectOut(unitIndex, effect, delay),
    )

    fun heights(
        scope: RoomScope,
        tiles: List<Point>,
    ) = MapMessages
        .heightMapUpdates(tiles.map { TileValue(it, RoomHeights.value(scope.room, it)) })
        .forEach { scope.out.send(Lane.AfterUnitUpdate, Audience.Room, HeightMapUpdateOutCodec, it) }
}
