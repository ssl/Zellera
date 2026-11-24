package zel.game.guilds

import java.util.concurrent.ConcurrentHashMap

/** Guilds held in memory while in use; idle ones without unsaved changes are dropped again. */
internal class GuildCache(
    private val clock: () -> Int,
) {
    private class Held(
        val guild: Guild,
        @Volatile var usedAt: Int,
    )

    private val held = ConcurrentHashMap<Int, Held>()

    fun getOrLoad(
        guildId: Int,
        load: (Int) -> Guild?,
    ): Guild? {
        val now = clock()
        val kept =
            held.compute(guildId) { _, current ->
                current?.also { it.usedAt = now } ?: load(guildId)?.let { Held(it, now) }
            }
        return kept?.guild
    }

    fun hold(guild: Guild) {
        held[guild.id] = Held(guild, clock())
    }

    fun remove(guildId: Int) {
        held.remove(guildId)
    }

    fun forgetIdle() {
        val before = clock() - IDLE_SECONDS
        held.values.removeIf { it.usedAt < before && !it.guild.hasUnsavedChanges }
    }

    fun flush(save: (Guild) -> Unit) {
        held.values.forEach { save(it.guild) }
        held.clear()
    }

    private companion object {
        const val IDLE_SECONDS = 30 * 60
    }
}
