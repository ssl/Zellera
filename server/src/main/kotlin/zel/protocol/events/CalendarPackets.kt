package zel.protocol.events

import zel.game.campaign.calendar.CampaignCalendars
import zel.protocol.generated.OpenCampaignCalendarDoorAsStaffInCodec
import zel.protocol.generated.OpenCampaignCalendarDoorInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute

class CalendarPackets(
    calendar: CampaignCalendars,
) : PacketPackage {
    override val name = "calendar"

    override val routes =
        listOf(
            PacketRoute(OpenCampaignCalendarDoorInCodec) { session, m ->
                calendar.openDoor(requireNotNull(session.client.player), m.campaignName, m.day, false)
            },
            PacketRoute(OpenCampaignCalendarDoorAsStaffInCodec) { session, m ->
                calendar.openDoor(requireNotNull(session.client.player), m.campaignName, m.day, true)
            },
        )
}
