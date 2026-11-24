package zel.game.pets

/** [face] is the value of the room's `gst` status. */
enum class PetMood(
    val face: String,
) {
    HAPPY("sml"),
    SAD("sad"),
    HUNGRY("hng"),
    THIRSTY("thr"),
}
