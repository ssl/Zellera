package zel.wired.chest

enum class ChestNotice(
    val kind: String,
) {
    FULL("chest_full"),
    DONATION("donation"),
    WITHDRAW("someone_withdraws"),
    EMPTY("chest_empty"),
    WIRED("chest_wired_transaction"),
    EARNINGS("earnings"),
    ;

    val reason: Int get() = ordinal
}
