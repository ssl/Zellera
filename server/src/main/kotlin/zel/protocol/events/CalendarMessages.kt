package zel.protocol.events

import zel.game.campaign.calendar.CalendarCampaign
import zel.net.clients.ClientConnection
import zel.protocol.generated.CampaignCalendarDataOut
import zel.protocol.generated.CampaignCalendarDataOutCodec
import zel.protocol.generated.CampaignCalendarDoorOpenedOut
import zel.protocol.generated.CampaignCalendarDoorOpenedOutCodec

class CalendarMessages {
    fun calendar(
        client: ClientConnection,
        campaign: CalendarCampaign,
        currentDay: Int,
        openedDays: List<Int>,
        shutDays: List<Int>,
    ) = client.send(
        CampaignCalendarDataOutCodec,
        CampaignCalendarDataOut(campaign.name, campaign.image, currentDay, campaign.days, openedDays, shutDays),
    )

    fun doorOpened(
        client: ClientConnection,
        visible: Boolean,
        productName: String,
        customImage: String,
        className: String,
    ) = client.send(
        CampaignCalendarDoorOpenedOutCodec,
        CampaignCalendarDoorOpenedOut(visible, productName, customImage, className),
    )
}
