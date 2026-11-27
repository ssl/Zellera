package zel.game.icons

import zel.content.ClientStrings

data class IconRoster(
    val sets: List<IconSet>,
    val icons: Map<Int, Icon>,
    val lastUsed: List<Int>,
    val unseen: List<Int>,
) {
    /** Points type 5 is the premium currency. */
    data class Price(
        val credits: Int,
        val points: Int,
        val pointsType: Int,
    ) {
        fun forSale(): Boolean = credits > 0 || points > 0

        companion object {
            val FREE = Price(0, 0, 0)
        }
    }

    data class Icon(
        val id: Int,
        val name: String,
        val setId: Int,
        val state: IconState,
        val price: Price = Price.FREE,
    )

    data class IconSet(
        val id: Int,
        val name: String,
        val completed: Boolean,
        val rewardId: Int,
        val rewardState: IconState,
        val icons: List<Icon>,
        val price: Price = Price.FREE,
    ) {
        /** The set's offer for what the player still lacks; null for a free set or one already gathered. */
        fun restOfSetOffer(): IconPurchase? {
            val lacking = icons.filter { it.state == IconState.PURCHASABLE }.map(Icon::id)
            if (!price.forSale() || lacking.isEmpty()) return null
            return IconPurchase(0, "${textPrefix()}collection_$name", price, lacking)
        }
    }

    fun requireIcon(id: Int): Icon = icons[id] ?: throw IconRejected(IconRejected.UNKNOWN)

    fun offerFor(id: Int): IconPurchase {
        val icon = requireIcon(id)
        if (icon.state != IconState.PURCHASABLE || !icon.price.forSale()) {
            throw IconRejected(IconRejected.UNAVAILABLE)
        }
        return IconPurchase(ICON_OFFER_BASE + id, "${textPrefix()}${icon.name}", icon.price, listOf(id))
    }

    fun offerForSet(setId: Int): IconPurchase {
        val set = sets.firstOrNull { it.id == setId } ?: throw IconRejected(IconRejected.UNKNOWN)
        return set.restOfSetOffer() ?: throw IconRejected(IconRejected.UNAVAILABLE)
    }

    fun isSetReward(iconId: Int): Boolean = iconId > 0 && sets.any { it.rewardId == iconId }

    fun changedSince(prior: IconRoster): List<Icon> = icons.values.filter { prior.requireIcon(it.id).state != it.state }

    class StoredIcon(
        val id: Int,
        val name: String,
        val setId: Int,
        val price: Price,
        val saved: IconState?,
        val unseen: Boolean,
        val ownedByDefault: Boolean,
        val available: Boolean,
        val isSetReward: Boolean,
    ) {
        fun state(allUnlocked: Boolean): IconState =
            when {
                saved != null -> saved
                allUnlocked || ownedByDefault -> IconState.OWNED
                isSetReward || !available -> IconState.UNAVAILABLE
                price.forSale() -> IconState.PURCHASABLE
                else -> IconState.LOCKED
            }
    }

    class StoredSet(
        val id: Int,
        val name: String,
        val rewardId: Int,
        val price: Price,
    )

    companion object {
        const val ICON_OFFER_BASE = 28970

        /** Starts the client's text keys of icons and sets; empty without a client profile. */
        fun textPrefix(): String = ClientStrings[ClientStrings.ICON_TEXT_PREFIX].orEmpty()

        /** Settles every icon's state, then the sets: a set gathered in full opens its reward. */
        fun assemble(
            stored: List<StoredIcon>,
            storedSets: List<StoredSet>,
            lastUsed: List<Int>,
            allUnlocked: Boolean,
        ): IconRoster {
            val icons = linkedMapOf<Int, Icon>()
            for (row in stored) icons[row.id] = Icon(row.id, row.name, row.setId, row.state(allUnlocked), row.price)
            val sets = storedSets.map { settleSet(it, icons) }
            return IconRoster(
                sets,
                icons.toMap(),
                lastUsed.filter { it in icons },
                stored.filter(StoredIcon::unseen).map(StoredIcon::id),
            )
        }

        private fun settleSet(
            set: StoredSet,
            icons: MutableMap<Int, Icon>,
        ): IconSet {
            val pieces = icons.values.filter { it.setId == set.id && it.id != set.rewardId }
            val completed = pieces.isNotEmpty() && pieces.all { it.state.countsForSet }
            val reward = icons[set.rewardId]
            val opened =
                reward?.takeIf { it.setId == set.id && completed && it.state == IconState.UNAVAILABLE }?.let {
                    it.copy(state = IconState.CLAIMABLE).also { earned -> icons[set.rewardId] = earned }
                }
            return IconSet(
                set.id,
                set.name,
                completed,
                set.rewardId,
                (opened ?: reward)?.state ?: IconState.UNAVAILABLE,
                pieces,
                restOfSetPrice(set.price, pieces),
            )
        }

        fun restOfSetPrice(
            setPrice: Price,
            pieces: List<Icon>,
        ): Price {
            if (!setPrice.forSale()) return setPrice
            val inHand = pieces.filter { it.state.countsForSet }
            return Price(
                maxOf(0, setPrice.credits - inHand.sumOf { it.price.credits }),
                maxOf(0, setPrice.points - inHand.sumOf { it.price.points }),
                setPrice.pointsType,
            )
        }

        fun newlyOwned(
            prior: IconRoster,
            changed: List<Icon>,
        ): Int = changed.count { it.state.owned && prior.icons[it.id]?.state?.owned != true }
    }
}
