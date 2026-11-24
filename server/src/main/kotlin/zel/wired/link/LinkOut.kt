package zel.wired.link

import zel.protocol.generated.PacketWriter
import zel.protocol.generated.WiredLinkConfigurationOut
import zel.protocol.generated.WiredLinkConfigurationOutCodec
import zel.protocol.generated.WiredLinkConnectionsOut
import zel.protocol.generated.WiredLinkConnectionsOutCodec
import zel.protocol.generated.WiredLinkInvalidatedOut
import zel.protocol.generated.WiredLinkInvalidatedOutCodec
import zel.protocol.generated.WiredLinkOperationResultOut
import zel.protocol.generated.WiredLinkOperationResultOutCodec
import zel.protocol.typed.PacketSession

object LinkOut {
    fun send(
        session: PacketSession,
        place: LinkPlace,
        reply: LinkReply,
    ) {
        val room = place.roomId
        val furni = place.furniId
        when (reply) {
            is LinkReply.Configuration -> {
                session.send(WiredLinkConfigurationOutCodec, configuration(room, furni, reply))
            }

            is LinkReply.Done -> {
                session.send(
                    WiredLinkOperationResultOutCodec,
                    WiredLinkOperationResultOut(room, furni, reply.operation, 0, true, done(reply)),
                )
            }

            is LinkReply.Refused -> {
                session.send(
                    WiredLinkOperationResultOutCodec,
                    WiredLinkOperationResultOut(
                        room,
                        furni,
                        reply.operation,
                        reply.result,
                        false,
                        REFUSED_TAIL,
                    ),
                )
            }

            is LinkReply.Connections -> {
                session.send(WiredLinkConnectionsOutCodec, connections(room, furni, reply))
            }

            is LinkReply.Invalidated -> {
                session.send(
                    WiredLinkInvalidatedOutCodec,
                    WiredLinkInvalidatedOut(room, reply.linkId, reply.networkId.toString()),
                )
            }
        }
    }

    private fun configuration(
        room: Int,
        furni: Int,
        reply: LinkReply.Configuration,
    ) = WiredLinkConfigurationOut(
        room,
        furni,
        wire(reply.setup.networkId),
        reply.setup.hearsOwnRoom,
        reply.rows.map {
            WiredLinkConfigurationOut.List1Item(it.networkId.toString(), it.name, it.count, it.linkedHere)
        },
    )

    private fun connections(
        room: Int,
        furni: Int,
        reply: LinkReply.Connections,
    ) = WiredLinkConnectionsOut(
        room,
        furni,
        reply.networkId.toString(),
        reply.rows.map {
            WiredLinkConnectionsOut.List1Item(it.roomId, it.roomName, it.linkId, OWNED_FURNI, it.hearsOwnRoom)
        },
    )

    private fun done(reply: LinkReply.Done): ByteArray {
        val tail =
            PacketWriter()
                .string(wire(reply.setup.networkId))
                .boolean(reply.setup.hearsOwnRoom)
                .string(wire(reply.removed))
                .int(reply.rows.size)
        reply.rows.forEach {
            tail
                .string(it.networkId.toString())
                .string(it.name)
                .int(it.count)
                .boolean(it.linkedHere)
        }
        return tail.toByteArray()
    }

    private fun wire(networkId: Int?): String = networkId?.toString() ?: LinkDialog.NONE

    private const val OWNED_FURNI = false

    private val REFUSED_TAIL: ByteArray =
        PacketWriter()
            .string(LinkDialog.NONE)
            .int(0)
            .toByteArray()
}
