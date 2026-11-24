package zel.game.players.subscriptions

/** Changes reach the table first and the product's reactions after. */
class Subscription internal constructor(
    row: SubscriptionRow,
    private val store: SubscriptionStore,
    private val clock: () -> Int,
    private val reactions: SubscriptionReactions = SubscriptionReactions.None,
) {
    val id: Int = row.id
    val playerId: Int = row.playerId
    val type: String = row.type
    val startedAt: Int = row.startedAt

    var duration: Int = row.duration
        private set

    var isActive: Boolean = row.active
        private set

    val endsAt: Int get() = startedAt + duration

    val secondsLeft: Int get() = endsAt - clock()

    fun started() = reactions.onStarted(this)

    /** [onRecorded] runs once the table holds the new length, before the product reacts. */
    fun extend(
        seconds: Int,
        onRecorded: () -> Unit,
    ) {
        duration += seconds
        store.saveDuration(id, duration)
        onRecorded()
        reactions.onExtended(this, seconds)
    }

    /** The product reacts while the membership still reads as active. */
    fun expire() {
        store.saveActive(id, false)
        reactions.onExpired(this)
        isActive = false
    }

    companion object {
        const val CLUB = "CLUB"
        const val VIP = "VIP"
        const val BUILD_PASS = "BUILD_PASS"
        const val BUILD_PASS_ADDON = "BUILD_PASS_ADDON"

        const val KIND_CLUB = "club"

        private val storedKinds =
            mapOf(CLUB to KIND_CLUB, VIP to "vip", BUILD_PASS to "build_pass", BUILD_PASS_ADDON to "build_pass_addon")

        fun storedKind(type: String): String =
            storedKinds[type]
                ?: type.takeIf { it in storedKinds.values }
                ?: throw IllegalArgumentException("no membership type $type")

        fun typeOfStored(kind: String): String =
            storedKinds.entries.firstOrNull { it.value == kind }?.key
                ?: throw IllegalArgumentException("no membership kind $kind")

        fun typeNamed(word: String): String? =
            word.takeIf { it in storedKinds } ?: storedKinds.entries.firstOrNull { it.value == word }?.key
    }
}
