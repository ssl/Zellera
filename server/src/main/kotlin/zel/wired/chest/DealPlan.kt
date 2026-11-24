package zel.wired.chest

import zel.game.items.CreditFurni
import zel.room.furni.RoomFurni

/** [of] gives null when some part cannot be met. */
class DealPlan(
    val sets: Int,
    val paid: List<OfferedFurni>,
    val leftOver: List<OfferedFurni>,
    val credits: Int,
    val paidFurni: List<OfferedFurni>,
    val coinChest: Pair<RoomFurni, Chest>?,
    val furniChest: Pair<RoomFurni, Chest>?,
    val furniOut: List<Pair<Pair<RoomFurni, Chest>, List<StoredFurni>>>,
    val coinsOut: List<Pair<Pair<RoomFurni, Chest>, Int>>,
) {
    val creditsOut: Int get() = coinsOut.sumOf { it.second }

    companion object {
        fun of(
            table: Table,
            deal: Deal.Negotiated,
            chests: List<Pair<RoomFurni, Chest>>,
        ): DealPlan? {
            val offered = table.offered.values.toList()
            val coins = chests.firstOrNull { it.second.isCoins }
            val furni = chests.firstOrNull { !it.second.isCoins }
            return if (deal.contract.isDonation) {
                donation(offered, coins, furni)
            } else {
                contractual(deal, offered, chests, coins, furni)
            }
        }

        private fun contractual(
            deal: Deal.Negotiated,
            offered: List<OfferedFurni>,
            chests: List<Pair<RoomFurni, Chest>>,
            coins: Pair<RoomFurni, Chest>?,
            furni: Pair<RoomFurni, Chest>?,
        ): DealPlan? {
            val (rule, covered) = Requirements.covered(deal.contract, offered)
            val sets = lowered(covered.coerceAtMost(Requirements.limit(deal)), rule + deal.contract.get)
            val (paid, leftOver) = taken(rule, sets, offered)
            val (paidCoin, paidFurni) = paid.partition { isCreditPayment(it, rule) }
            val out = rewards(deal.contract.get, sets, chests)
            val stores = (paidCoin.isEmpty() || coins != null) && (paidFurni.isEmpty() || furni != null)
            val meets = sets >= Requirements.needed(deal).coerceAtMost(covered)
            return if (out != null && stores && meets) {
                DealPlan(
                    sets,
                    paid,
                    leftOver,
                    paidCoin.sumOf { CreditFurni.credits(it.name) },
                    paidFurni,
                    coins,
                    furni,
                    out.first,
                    out.second,
                )
            } else {
                null
            }
        }

        private fun isCreditPayment(
            item: OfferedFurni,
            rule: List<Term>,
        ): Boolean =
            CreditFurni.credits(item.name) > 0 &&
                rule.none { !it.credits && it.wall == item.wall && it.typeId == item.spriteId }

        private fun donation(
            offered: List<OfferedFurni>,
            coins: Pair<RoomFurni, Chest>?,
            furni: Pair<RoomFurni, Chest>?,
        ): DealPlan? {
            val (paidCoin, paidFurni) = offered.partition { coins != null && CreditFurni.credits(it.name) > 0 }
            if (paidFurni.isNotEmpty() && furni == null) return null
            return DealPlan(
                1,
                offered,
                emptyList(),
                paidCoin.sumOf { CreditFurni.credits(it.name) },
                paidFurni,
                coins,
                furni,
                emptyList(),
                emptyList(),
            )
        }

        fun lowered(
            sets: Int,
            terms: List<Term>,
        ): Int =
            terms.fold(sets) { lowest, term ->
                minOf(lowest, Term.ceiling(term.credits) / term.amount.coerceAtLeast(1))
            }

        private fun taken(
            rule: List<Term>,
            sets: Int,
            offered: List<OfferedFurni>,
        ): Pair<List<OfferedFurni>, List<OfferedFurni>> {
            val paid = LinkedHashSet<OfferedFurni>()
            rule.filterNot { it.credits }.forEach { term ->
                paid += offered.filter { it.wall == term.wall && it.spriteId == term.typeId }.take(term.amount * sets)
            }
            if (rule.any { it.credits }) paid += offered.filter { isCreditPayment(it, rule) }
            return paid.toList() to offered.filterNot { it in paid }
        }

        fun rewards(
            get: List<Term>,
            sets: Int,
            chests: List<Pair<RoomFurni, Chest>>,
        ): Pair<List<Pair<Pair<RoomFurni, Chest>, List<StoredFurni>>>, List<Pair<Pair<RoomFurni, Chest>, Int>>>? {
            val furniOut = LinkedHashMap<Int, MutableList<StoredFurni>>()
            val taken = HashSet<Int>()
            val furniChests = chests.filter { !it.second.isCoins }
            get.filterNot { it.credits }.forEach { term ->
                var missing = term.amount * sets
                furniChests.forEach { (_, chest) ->
                    val found =
                        chest.furni
                            .filter {
                                it.isWall == term.wall && it.spriteId == term.typeId && it.slotId !in taken
                            }.take(missing)
                    if (found.isNotEmpty()) furniOut.getOrPut(chest.id) { ArrayList() } += found
                    found.forEach { taken += it.slotId }
                    missing -= found.size
                }
                if (missing > 0) return null
            }
            val wanted = get.filter { it.credits }.sumOf { it.amount.toLong() * sets }
            var left = wanted
            val coinsOut = ArrayList<Pair<Pair<RoomFurni, Chest>, Int>>()
            chests.filter { it.second.isCoins }.forEach { pair ->
                val part = minOf(left, pair.second.credits.toLong()).toInt()
                if (part > 0) coinsOut += pair to part
                left -= part
            }
            if (left > 0) return null
            return furniChests.mapNotNull { pair -> furniOut[pair.second.id]?.let { pair to it.toList() } } to coinsOut
        }

        fun rowType(contract: Contract): Int =
            when {
                contract.kind == ContractKind.TRADE -> TransactionRow.TRADE
                contract.kind == ContractKind.REWARD || contract.paysAtOnce -> TransactionRow.REWARD
                else -> TransactionRow.PAYMENT
            }

        fun definition(
            deal: Deal.Negotiated,
            sets: Int,
        ): String =
            when (deal.mode) {
                Requirements.MODE_FIXED -> "contract id: ${deal.contractId}, multiplier: $sets"
                Requirements.MODE_AUTO -> "contract id: ${deal.contractId}, auto multiplier: $sets"
                else -> "contract id: ${deal.contractId}"
            }
    }
}
