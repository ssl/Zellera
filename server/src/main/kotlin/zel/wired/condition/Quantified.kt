package zel.wired.condition

import zel.wired.box.BoxSettings
import zel.wired.box.WiredBox
import zel.wired.firing.FurniSource
import zel.wired.firing.UserSource

internal object Quantified {
    private const val ANY = 1

    /** User sources that name the event's own user: without one the box still fails, it has the event to check. */
    private val EVENT_USERS = setOf(UserSource.TRIGGERING, UserSource.REACHED, UserSource.CLICKED)

    /**
     * True when the box's user slot is a set (selector, signal, the room's users): an empty one is read by the
     * box's quantifier ([noUsers]). An absent event user (0, 10, 11) fails the box and its not box alike.
     */
    fun noUsersPass(
        box: WiredBox,
        slot: Int = 0,
    ): Boolean = (box.settings.userSources.getOrNull(slot) ?: UserSource.TRIGGERING) !in EVENT_USERS

    /**
     * True when the box's furni slot is a set (picks, selector, signal, the room's furni): an empty one is read by
     * the box's quantifier ([noFurni]). An absent triggering furni (0) keeps the box's own reading.
     */
    fun noFurniPass(
        box: WiredBox,
        slot: Int = 0,
    ): Boolean = ConditionTargets.sourceOf(box, slot) != FurniSource.TRIGGERING

    /**
     * An empty set: "all" holds (nothing fails it), "any" does not (nothing matches); a not box is the positive
     * box turned around, so it fails under "all" and passes under "any".
     */
    fun emptyHolds(
        all: Boolean,
        negative: Boolean,
    ): Boolean = all != negative

    fun emptyHolds(
        settings: BoxSettings,
        negative: Boolean,
    ): Boolean = emptyHolds(settings.quantifier != ANY, negative)

    /** The result for an empty user slot: the quantifier's reading for a set, a fail for an absent event user. */
    fun noUsers(
        box: WiredBox,
        negative: Boolean = false,
        slot: Int = 0,
    ): Boolean = noUsersPass(box, slot) && emptyHolds(box.settings, negative)

    /** The result for an empty furni slot: the quantifier's reading for a set, a fail for the triggering furni. */
    fun noFurni(
        box: WiredBox,
        negative: Boolean = false,
        slot: Int = 0,
    ): Boolean = noFurniPass(box, slot) && emptyHolds(box.settings, negative)

    /** No targets never pass; callers decide first whether an empty set is nothing to check. */
    fun <T> holds(
        targets: Collection<T>,
        quantifier: Int,
        test: (T) -> Boolean,
    ): Boolean = targets.isNotEmpty() && if (quantifier == ANY) targets.any(test) else targets.all(test)

    fun <T> holds(
        targets: Collection<T>,
        settings: BoxSettings,
        test: (T) -> Boolean,
    ): Boolean = holds(targets, settings.quantifier, test)

    fun <T> perTarget(
        targets: Collection<T>,
        all: Boolean,
        negative: Boolean,
        test: (T) -> Boolean,
    ): Boolean =
        when {
            !negative -> targets.isNotEmpty() && if (all) targets.all(test) else targets.any(test)
            targets.isEmpty() -> true
            all -> targets.none(test)
            else -> targets.any { !test(it) }
        }
}
