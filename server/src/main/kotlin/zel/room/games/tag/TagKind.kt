package zel.room.games.tag

@Suppress("MagicNumber") // the effect ids each game's furni give
enum class TagKind(
    val scoreName: String,
    val runner: TagLook,
    val tagger: TagLook,
    val hasPoles: Boolean,
    /** The detail of this game's tag events. */
    val detail: String,
) {
    IceTag("IceTagGame", TagLook(38, 39), TagLook(45, 46), true, "ice"),
    Bunnyrun("BunnyrunGame", TagLook(0, 0), TagLook(68, 68), true, "bunny"),
    Rollerskate("RollerskateGame", TagLook(55, 56), TagLook(57, 58), false, "rollerskate"),
}
