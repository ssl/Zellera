package zel.protocol.outgoing.generic.alerts

/** The text is under `notification.<key>`. */
enum class BubbleAlert(
    val key: String,
) {
    BUILD_PASS_JOINED("build_pass.membership_made"),
    BUILD_PASS_EXTENDED("build_pass.membership_extended"),
    BUILD_PASS_ROOM_OPENED("build_pass.room_unlocked"),
    BUILD_PASS_OWNER_KEPT_OUT("build_pass.visit_denied_for_owner"),
    FORUM_CLOSED_TO_CALLER("forums.error.access_denied"),
    FORUM_SETTINGS_SAVED("forums.forum_settings_updated"),
    POST_HIDDEN("forums.message.hidden"),
    POST_RESTORED("forums.message.restored"),
    THREAD_HIDDEN("forums.thread.hidden"),
    THREAD_RESTORED("forums.thread.restored"),
    THREAD_LOCKED("forums.thread.locked"),
    THREAD_UNLOCKED("forums.thread.unlocked"),
    THREAD_PINNED("forums.thread.pinned"),
    THREAD_UNPINNED("forums.thread.unpinned"),
    EFFECT_ALREADY_OWNED("permanent_effect_exists"),
    ROOM_BOUGHT("purchasing.room"),
    GIFT_RECEIVED("received_gift"),
}
