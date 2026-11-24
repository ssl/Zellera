package zel.wired.signal

import zel.protocol.generated.ObjectDataUpdateOut
import zel.protocol.generated.ObjectDataUpdateOutCodec
import zel.room.RoomScope
import zel.room.furni.place.FurniWire
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.WiredRooms
import zel.wired.firing.Firing
import zel.wired.firing.FiringStart
import zel.wired.timing.RoomPoster

class LinkMessages(
    private val rooms: () -> WiredRooms?,
    private val poster: RoomPoster,
    private val networks: LinkNetworks,
    private val heard: (RoomScope, Int, LinkMail) -> Unit = { _, _, _ -> },
) {
    fun send(
        firing: Firing,
        linkId: Int,
        sent: LinkMail,
    ) {
        val mail = sent.bounded()
        val scope = firing.scope
        flip(scope, linkId)
        val here = scope.room.info.id
        networks.members(linkId).forEach { member ->
            if (member.roomId != here) {
                poster.post(member.roomId, 0) { target ->
                    if (target.room.furni[member.linkId] != null) {
                        flip(target, member.linkId)
                        deliver(target, member.linkId, mail, null, here)
                    }
                }
            } else if (member.hearsOwnRoom) {
                if (member.linkId != linkId) flip(scope, member.linkId)
                deliver(scope, member.linkId, mail, firing, here)
            }
        }
    }

    private fun deliver(
        scope: RoomScope,
        linkId: Int,
        mail: LinkMail,
        sender: Firing?,
        sourceRoom: Int,
    ) {
        if (WiredRooms.isOff(scope.room)) return
        heard(scope, linkId, mail)
        val wired = rooms() ?: return
        wired.runEach(scope, sender?.event) { stack ->
            (stack.trigger as? LinkTrigger)
                ?.takeIf { it.listens(linkId) }
                ?.payload(mail)
                ?.let {
                    val context = it.contextValues + (SOURCE_ROOM to sourceRoom.toLong())
                    FiringStart(null, linkId, signal = it.copy(through = linkId, contextValues = context))
                }
        }
    }

    private fun flip(
        scope: RoomScope,
        linkId: Int,
    ) {
        val link = scope.room.furni[linkId] ?: return
        link.state = if (link.state == ON) OFF else ON
        scope.out.send(
            Lane.Now,
            Audience.Room,
            ObjectDataUpdateOutCodec,
            ObjectDataUpdateOut(linkId.toString(), FurniWire.stuff(link)),
        )
    }

    companion object {
        const val LINK = "wf_link"

        private const val SOURCE_ROOM = "-453"
        private const val ON = "1"
        private const val OFF = "0"
    }
}
