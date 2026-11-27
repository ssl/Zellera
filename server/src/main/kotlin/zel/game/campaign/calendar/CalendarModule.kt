package zel.game.campaign.calendar

import org.slf4j.LoggerFactory
import zel.core.Configuration
import zel.db.Database
import zel.game.items.Items
import zel.protocol.events.CalendarMessages
import zel.protocol.events.CalendarPackets
import zel.protocol.typed.PacketPackage
import java.util.Random

class CalendarModule(
    database: Database,
    items: Items,
    randomSource: () -> Random,
    unixTime: () -> Int,
    config: Configuration,
) {
    val calendars =
        CampaignCalendars(
            CalendarStore(database),
            items,
            randomSource,
            unixTime,
            CalendarMessages(),
            isActiveCampaign = { name ->
                config.getBoolean("calendar.is_open") && config.getValue("calendar.campaign") == name
            },
        )
    val packets: PacketPackage = CalendarPackets(calendars)

    init {
        val millis = System.currentTimeMillis()
        calendars.refresh()
        LoggerFactory
            .getLogger(CalendarModule::class.java)
            .info("calendars loaded in {} ms", System.currentTimeMillis() - millis)
    }
}
