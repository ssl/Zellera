package zel.wired.effect.bot

import zel.game.players.figure.LookValidation
import zel.protocol.generated.RoomUnitChangeOut
import zel.protocol.generated.RoomUnitChangeOutCodec
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.bot.RoomBots
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import java.util.Locale

class BotClothes(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val bots: RoomBots,
    private val normalized: (String) -> String = LookValidation::normalizeFigure,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val saved = BotChoice.rest(settings.text)
        if (!valid(saved)) return
        val figure = fit(normalized(saved))
        BotChoice.bots(firing, this, bots).forEach { (bot, unit) ->
            bot.bot.figure = figure
            bots.changed(firing.scope, bot)
            firing.scope.out.send(
                Lane.Now,
                Audience.Room,
                RoomUnitChangeOutCodec,
                RoomUnitChangeOut(
                    unit.index,
                    figure,
                    bot.bot.gender.name
                        .lowercase(Locale.ROOT),
                    bot.bot.motto,
                    0,
                    "",
                    emptyList(),
                    0,
                ),
            )
        }
    }

    companion object {
        private val FIGURE = Regex("""[a-z]{2}-\d+(-\d+)*(\.[a-z]{2}-\d+(-\d+)*)*""")

        private const val FIGURE_COLUMN_WIDTH = 500

        fun valid(figure: String): Boolean = FIGURE.matches(figure)

        fun fit(figure: String): String {
            if (figure.length <= FIGURE_COLUMN_WIDTH) return figure
            val kept = StringBuilder()
            for (set in figure.split('.')) {
                val length = kept.length + set.length + if (kept.isEmpty()) 0 else 1
                if (length > FIGURE_COLUMN_WIDTH) break
                if (kept.isNotEmpty()) kept.append('.')
                kept.append(set)
            }
            return kept.toString()
        }
    }
}
