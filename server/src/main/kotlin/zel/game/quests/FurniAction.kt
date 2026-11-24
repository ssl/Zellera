package zel.game.quests

enum class FurniAction(
    internal val goal: CountedAction,
) {
    CLICK(CountedAction.CLICK_FURNI),

    USE(CountedAction.FIND_STUFF),

    STEP(CountedAction.WALK_OVER_STUFF),
}
