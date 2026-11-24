package zel.game.achievements.resolution

import zel.game.achievements.Achievement
import zel.game.players.Player
import zel.util.SECONDS_PER_DAY

class Resolutions(
    private val store: ResolutionStore,
    private val days: () -> Int,
    private val achievementById: (Int) -> Achievement?,
) {
    /** [achievementId] 0 only looks. */
    fun open(
        player: Player,
        furniId: Int,
        achievementId: Int,
        now: Int,
    ): ResolutionState {
        val stored = store.byFurni(furniId)
        if (stored != null && stored.isExpired(now)) store.delete(furniId)
        val held = stored?.takeUnless { it.isExpired(now) }
        return when {
            held == null -> promise(player, furniId, achievementId, now) ?: picker(player)
            held.isCompleted -> ResolutionState.Completed(held, badgeOf(held))
            else -> progressOf(player, held, now)
        }
    }

    fun reset(furniId: Int) {
        store.byFurni(furniId)?.takeUnless { it.isCompleted }?.let { store.delete(furniId) }
    }

    private fun promise(
        player: Player,
        furniId: Int,
        achievementId: Int,
        now: Int,
    ): ResolutionState? {
        val choice =
            choices(player).firstOrNull { it.achievementId == achievementId && it.state == ResolutionChoice.State.OPEN }
                ?: return null
        val resolution = Resolution(furniId, player.profile.id, achievementId, choice.requiredLevel, now + duration())
        if (!store.add(resolution)) return null
        return progressOf(player, resolution, now)
    }

    private fun picker(player: Player) = ResolutionState.Picking(choices(player), duration())

    private fun progressOf(
        player: Player,
        resolution: Resolution,
        now: Int,
    ): ResolutionState {
        val achievement = achievementById(resolution.achievement)
        val needed = achievement?.levels?.get(resolution.level)?.progressNeeded ?: 0
        val current = maxOf(0, player.stats.progressOf(achievement))
        val badge = badgeOf(resolution)
        if (current >= needed) {
            store.markCompleted(resolution.furniId, now)
            return ResolutionState.Completed(resolution.copy(completedAt = now), badge)
        }
        val progress = ResolutionProgress(current, needed)
        return ResolutionState.Running(resolution, badge, progress, resolution.remaining(now))
    }

    private fun badgeOf(resolution: Resolution): String =
        achievementById(resolution.achievement)?.badgeCode(resolution.level).orEmpty()

    private fun duration(): Int = days() * SECONDS_PER_DAY

    private fun choices(player: Player): List<ResolutionChoice> {
        val stats = player.stats
        val started = synchronized(stats.achievementProgress) { stats.achievementProgress.filterValues { it > 0 } }
        val promised = store.openAchievementIds(player.profile.id)
        return started.entries
            .sortedBy { (achievement, _) -> achievement.id }
            .mapNotNull { (achievement, progress) -> choice(achievement, progress, achievement.id in promised) }
    }

    private fun choice(
        achievement: Achievement,
        progress: Int,
        promised: Boolean,
    ): ResolutionChoice? {
        val reached = achievement.levelAt(progress)?.level ?: 0
        val next = achievement.levelAfter(reached)
        val target = next ?: achievement.lastLevel ?: return null
        val state =
            when {
                next == null -> ResolutionChoice.State.FINISHED
                promised -> ResolutionChoice.State.TAKEN
                else -> ResolutionChoice.State.OPEN
            }
        return ResolutionChoice(achievement.id, reached, achievement.badgeCode(target.level), target.level, state)
    }
}
