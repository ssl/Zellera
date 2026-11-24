package zel.game.pets

import zel.core.Configuration
import zel.core.Texts
import zel.core.threading.GameThreads
import zel.game.achievements.Achievements
import zel.game.items.Items
import zel.game.players.Players
import java.util.Random
import java.util.function.IntSupplier
import java.util.function.Supplier
import javax.sql.DataSource

/** What the pet package needs from the rest of the game; services built after the pets resolve when asked for. */
class PetServices(
    val threading: GameThreads,
    val config: Configuration,
    val texts: Texts,
    val dataSource: DataSource,
    private val random: Supplier<Random>,
    private val clock: IntSupplier,
    private val achievements: Supplier<Achievements>,
    private val players: Supplier<Players>,
    private val items: Supplier<Items>,
) {
    /** Unix seconds. */
    fun now(): Int = clock.asInt

    fun random(): Random = random.get()

    fun achievements(): Achievements = achievements.get()

    fun players(): Players = players.get()

    fun items(): Items = items.get()
}
