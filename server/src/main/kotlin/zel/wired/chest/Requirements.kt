package zel.wired.chest

import zel.game.items.CreditFurni

object Requirements {
    fun covered(
        contract: Contract,
        offered: Collection<OfferedFurni>,
    ): Pair<List<Term>, Int> =
        contract.give
            .map { it to sets(it, offered) }
            .firstOrNull { it.second > 0 }
            ?: (emptyList<Term>() to 0)

    fun sets(
        rule: List<Term>,
        offered: Collection<OfferedFurni>,
    ): Int {
        if (rule.isEmpty()) return 0
        val furniTerms = rule.filterNot { it.credits }
        val named = furniTerms.map { it.wall to it.typeId }.toSet()
        val credits =
            offered.filter { (it.wall to it.spriteId) !in named }.sumOf {
                CreditFurni
                    .credits(
                        it.name,
                    ).toLong()
            }
        return rule.minOf { term ->
            val held =
                if (term.credits) {
                    credits
                } else {
                    offered
                        .count { it.wall == term.wall && it.spriteId == term.typeId }
                        .toLong()
                }
            (held / term.amount.coerceAtLeast(1)).toInt()
        }
    }

    fun limit(deal: Deal.Negotiated): Int =
        when (deal.mode) {
            MODE_FIXED, MODE_AUTO -> deal.multiplier
            else -> 1
        }

    fun needed(deal: Deal.Negotiated): Int = if (deal.mode == MODE_FIXED) deal.multiplier else 1

    const val MODE_FIXED = 1
    const val MODE_AUTO = 2
}
