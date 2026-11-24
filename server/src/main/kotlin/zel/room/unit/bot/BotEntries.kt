package zel.room.unit.bot

import zel.game.items.FurniHeights
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.RoomUnitsOut
import zel.room.unit.RoomUnit

object BotEntries {
    private const val BOT_ENTRY = 4

    /** A public bot's entry ends at the entry type: no gender, owner or skills. */
    private const val PUBLIC_BOT_ENTRY = 3

    private val SKILLS: List<Short> = listOf(1, 2, 3, 4, 5, 19, 20, 21, 22, 23)

    fun users(bots: List<Pair<RoomBot, RoomUnit>>): RoomUnitsOut {
        val w = PacketWriter().int(bots.size)
        bots.forEach { (bot, unit) -> write(w, bot, unit) }
        return RoomUnitsOut(w.toByteArray())
    }

    private fun write(
        w: PacketWriter,
        roomBot: RoomBot,
        unit: RoomUnit,
    ) {
        val bot = roomBot.bot
        w
            .int(-bot.id)
            .string(bot.name)
            .string(bot.motto)
            .string(bot.figure)
            .int(unit.index)
            .int(unit.tile.x)
            .int(unit.tile.y)
            .string(FurniHeights.formatZ(unit.z))
            .int(unit.bodyDirection)
        if (bot.isPublic) {
            w.int(PUBLIC_BOT_ENTRY)
            return
        }
        w
            .int(BOT_ENTRY)
            .string(bot.gender.name.lowercase())
            .int(bot.ownerId)
            .string(bot.ownerName.orEmpty())
        w.int(SKILLS.size)
        SKILLS.forEach { w.short(it) }
    }
}
