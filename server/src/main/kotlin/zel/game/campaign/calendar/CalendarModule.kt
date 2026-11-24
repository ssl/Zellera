package zel.game.campaign.calendar

import org.slf4j.LoggerFactory
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
) {
    val calendars = CampaignCalendars(CalendarStore(database), items, randomSource, unixTime, CalendarMessages())
    val packets: PacketPackage = CalendarPackets(calendars)

    init {
        val millis = System.currentTimeMillis()
        calendars.refresh()
        LoggerFactory
            .getLogger(CalendarModule::class.java)
            .info("calendars loaded in {} ms", System.currentTimeMillis() - millis)
    }
}
