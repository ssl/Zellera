package zel.room.furni.buildpass

import zel.protocol.generated.BuildPassFurniCountOutCodec
import zel.protocol.generated.BuildPassSubscriptionStatusOutCodec
import zel.protocol.generated.NotificationDialogOut
import zel.protocol.generated.NotificationDialogOutCodec
import zel.protocol.generated.RoomChatSettingsOut
import zel.protocol.generated.RoomChatSettingsOutCodec
import zel.protocol.generated.RoomInfoUpdatedOut
import zel.protocol.generated.RoomInfoUpdatedOutCodec
import zel.protocol.generated.RoomVisualizationSettingsOutCodec
import zel.protocol.outgoing.catalog.CatalogMessages
import zel.room.RoomScope
import zel.room.map.MapMessages
import zel.room.tick.Audience
import zel.room.tick.Lane

object BuildPassOut {
    fun count(
        scope: RoomScope,
        playerId: Int,
        count: Int,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        BuildPassFurniCountOutCodec,
        CatalogMessages.buildPassFurniCount(count),
    )

    fun status(
        scope: RoomScope,
        account: BuildPassAccount,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(account.playerId),
        BuildPassSubscriptionStatusOutCodec,
        CatalogMessages.buildPassStatus(account.secondsLeft, account.limit, account.maxLimit, account.secondsLeft),
    )

    fun notice(
        scope: RoomScope,
        playerId: Int,
        type: String,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        NotificationDialogOutCodec,
        NotificationDialogOut(type, emptyList()),
    )

    fun roomSettings(scope: RoomScope) {
        val info = scope.room.info
        val out = scope.out
        out.send(Lane.Now, Audience.Room, RoomInfoUpdatedOutCodec, RoomInfoUpdatedOut(info.id))
        out.send(
            Lane.Now,
            Audience.Room,
            RoomVisualizationSettingsOutCodec,
            MapMessages.visualization(info.visuals),
        )
        out.send(
            Lane.Now,
            Audience.Room,
            RoomChatSettingsOutCodec,
            RoomChatSettingsOut(info.moderation.floodFilter),
        )
    }
}
