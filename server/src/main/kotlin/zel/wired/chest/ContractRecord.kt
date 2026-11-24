package zel.wired.chest

data class ContractRecord(
    val payment: PaymentForm,
    val reward: RewardNotice,
    val terms: List<TermRow>,
) {
    data class TermRow(
        val side: Int,
        val rule: Int,
        val position: Int,
        val credits: Boolean,
        val amount: Int,
        val definitionId: Int?,
        val unknownSpriteId: Int?,
        val posterCode: String,
    ) {
        fun toTerm(types: FurniTypes): Term {
            val definition = definitionId?.takeIf { it != 0 }
            return Term(
                credits = credits,
                amount = amount,
                wall = definition != null && types.isWall(definition),
                typeId = definition?.let(types::spriteOf) ?: unknownSpriteId ?: 0,
                extra = posterCode,
            )
        }

        companion object {
            fun of(
                term: Term,
                side: Int,
                rule: Int,
                position: Int,
                types: FurniTypes,
            ): TermRow {
                val furni = !term.credits && term.typeId != 0
                val definition = if (furni) types.baseOf(term.wall, term.typeId) else null
                return TermRow(
                    side = side,
                    rule = rule,
                    position = position,
                    credits = term.credits,
                    amount = term.amount,
                    definitionId = definition,
                    unknownSpriteId = term.typeId.takeIf { furni && definition == null },
                    posterCode = if (term.credits) "" else term.extra,
                )
            }
        }
    }

    interface FurniTypes {
        fun spriteOf(baseId: Int): Int

        fun baseOf(
            wall: Boolean,
            spriteId: Int,
        ): Int?

        fun isWall(baseId: Int): Boolean
    }

    fun toContract(
        kind: ContractKind,
        types: FurniTypes,
    ): Contract {
        val (give, get) = terms.partition { it.side == GIVE_SIDE }
        return Contract(
            kind = kind,
            give =
                give
                    .sortedWith(compareBy(TermRow::rule, TermRow::position))
                    .groupBy(TermRow::rule)
                    .values
                    .map { rule -> rule.map { it.toTerm(types) } },
            get = get.sortedBy(TermRow::position).map { it.toTerm(types) },
            payment = payment,
            reward = reward,
        )
    }

    companion object {
        const val GIVE_SIDE = 0
        const val GET_SIDE = 1

        fun of(
            contract: Contract,
            types: FurniTypes,
        ): ContractRecord {
            val give =
                contract.give.flatMapIndexed { rule, terms ->
                    terms.mapIndexed { position, term -> TermRow.of(term, GIVE_SIDE, rule, position, types) }
                }
            val get = contract.get.mapIndexed { position, term -> TermRow.of(term, GET_SIDE, 0, position, types) }
            return ContractRecord(contract.payment, contract.reward, give + get)
        }
    }
}
