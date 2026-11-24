package zel.wired.chest

import zel.protocol.generated.PacketWriter
import zel.protocol.generated.WiredContractContentsOut

object ContractWire {
    private const val NODE_CREDITS: Byte = 0
    private const val NODE_FURNI: Byte = 1

    fun rule(
        w: PacketWriter,
        rule: List<Term>,
    ) {
        w.int(rule.size)
        rule.forEach { node(w, it) }
    }

    private fun node(
        w: PacketWriter,
        term: Term,
    ) {
        if (term.credits) {
            w.byte(NODE_CREDITS).int(term.amount)
        } else {
            w
                .byte(NODE_FURNI)
                .int(term.amount)
                .boolean(term.wall)
                .int(term.typeId)
                .string(term.extra)
        }
    }

    fun sides(
        w: PacketWriter,
        contract: Contract,
        givesList: Boolean = contract.kind != ContractKind.REWARD,
        getsList: Boolean = contract.kind != ContractKind.PAYMENT,
    ) {
        w.boolean(givesList)
        if (givesList) {
            w.int(contract.give.size)
            contract.give.forEach { rule(w, it) }
        }
        w.boolean(getsList)
        if (getsList) rule(w, contract.get)
    }

    fun contents(
        id: Int,
        contract: Contract,
    ): WiredContractContentsOut {
        val full = PacketWriter()
        sides(full, contract)
        settings(full, contract)
        val bytes = full.toByteArray()
        return WiredContractContentsOut(
            id,
            contract.kind.wire.toShort(),
            bytes[0].toInt() != 0,
            bytes.copyOfRange(1, bytes.size),
        )
    }

    private fun settings(
        w: PacketWriter,
        contract: Contract,
    ) {
        when (contract.kind) {
            ContractKind.PAYMENT -> {
                val form = contract.payment
                w.short(form.mode.toShort()).string(form.receiveText).string(form.layout)
            }

            ContractKind.REWARD -> {
                val notice = contract.reward
                w.short(notice.category.toShort()).boolean(notice.popup).string(notice.text)
            }

            ContractKind.TRADE -> {}
        }
    }
}
