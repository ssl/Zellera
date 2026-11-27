package zel.game.players.figure

import org.slf4j.LoggerFactory
import zel.game.players.Player
import java.util.concurrent.atomic.AtomicReference

/**
 * Looks are checked against the figure data at four moments, each a setting, all on unless switched off. Without
 * any figure data there is nothing to check against; with figure data that cannot be read no look changes.
 */
object LookValidation {
    data class Settings(
        val onClubExpired: Boolean = true,
        val onLogin: Boolean = true,
        val onChangeLooks: Boolean = true,
        val onFootballGate: Boolean = true,
        val clubItemsFree: Boolean = true,
        val ownedItemsOnly: Boolean = false,
        val url: String = "",
        val figuredata: Figuredata = Figuredata(),
    ) {
        fun allOff() = copy(onClubExpired = false, onLogin = false, onChangeLooks = false, onFootballGate = false)

        /** Figure data is named and unreadable. */
        val isUnreadable: Boolean get() = url.isNotEmpty() && figuredata.isEmpty
    }

    private val log = LoggerFactory.getLogger(LookValidation::class.java)
    private val settings = AtomicReference(Settings().allOff())

    val validateOnHcExpire: Boolean get() = settings.get().onClubExpired
    val validateOnLogin: Boolean get() = settings.get().onLogin
    val validateOnChangeLooks: Boolean get() = settings.get().onChangeLooks
    val validateOnFballgate: Boolean get() = settings.get().onFootballGate

    /** True while the figure data cannot be read: a look nobody can check is not taken. */
    val refusesNewLooks: Boolean get() = settings.get().isUnreadable

    fun configure(
        triggers: Settings,
        url: String,
    ) {
        val current = settings.get()
        val known = current.figuredata.takeIf { url == current.url && !it.isEmpty }
        val next = triggers.copy(url = url, figuredata = known ?: tryRead(url))
        when {
            url.isEmpty() -> {
                log.warn("No figure data is installed: looks are taken unchecked until it is")
                settings.set(next.allOff())
            }

            next.isUnreadable -> {
                log.error("The figure data at {} cannot be read: no player can change look until it can", url)
                settings.set(next)
            }

            else -> {
                settings.set(next)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught") // whatever stops the read, the figure data counts as unreadable
    private fun tryRead(url: String): Figuredata =
        if (url.isEmpty()) {
            Figuredata()
        } else {
            try {
                FiguredataParser.read(url)
            } catch (e: Exception) {
                log.error("Reading the figure data at {} failed: {}", url, e.message)
                Figuredata()
            }
        }

    fun normalizeFigure(look: String): String = FigureNormalizer.normalize(settings.get().figuredata, look)

    fun allowedLook(player: Player): String = allowedLook(player, player.profile.figure, player.profile.gender.name)

    /** The part of [look] its wearer may wear; the look worn now while the figure data cannot be read. */
    fun allowedLook(
        player: Player,
        look: String,
        gender: String,
    ): String {
        val current = settings.get()
        if (current.isUnreadable) return player.profile.figure
        val owned =
            player.inventory.wardrobe.clothingSets
                .toList()
        val wearer =
            Figuredata.Wearer(
                gender,
                current.clubItemsFree || player.stats.club.isMember,
                owned,
                !current.ownedItemsOnly,
            )
        return Figure.clampToMaxLength(LookCleaner(current.figuredata).clean(look, wearer))
    }
}
