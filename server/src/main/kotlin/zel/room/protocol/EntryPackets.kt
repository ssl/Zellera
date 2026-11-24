package zel.room.protocol

import zel.protocol.generated.AdmitPlayerInCodec
import zel.protocol.generated.ChangeQueueInCodec
import zel.protocol.generated.OpenFlatConnectionInCodec
import zel.protocol.generated.QuitInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.room.entry.RoomEntry

class EntryPackets(
    entry: RoomEntry,
) : PacketPackage {
    override val name = "entry"
    override val routes =
        listOf(
            PacketRoute(OpenFlatConnectionInCodec) { session, m ->
                session.client.player?.let { entry.open(it, m.int1, m.string1) }
            },
            PacketRoute(AdmitPlayerInCodec) { session, m ->
                session.client.player?.let { entry.answer(it, m.string1, m.boolean1) }
            },
            PacketRoute(ChangeQueueInCodec) { session, m ->
                session.client.player?.let { entry.changeQueue(it, m.target) }
            },
            PacketRoute(QuitInCodec) { session, _ ->
                session.client.player?.let(entry::quit)
            },
        )
}
