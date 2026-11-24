package zel.wired.chest

import zel.protocol.generated.IncomingCodec
import zel.protocol.generated.PacketReader
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.WiredUpdateContractInCodec
import java.nio.BufferUnderflowException

object ContractSaveCodec : IncomingCodec<ContractSave> {
    private const val NODE_CREDITS = 0
    private const val NODE_FURNI = 1
    private const val KNOWN_MODES = 2

    override val name = WiredUpdateContractInCodec.name

    private class BrokenRule : RuntimeException()

    override fun read(r: PacketReader): ContractSave {
        val id = r.int()
        val type = r.short().toInt()
        return try {
            readBody(r, id, type)
        } catch (_: BrokenRule) {
            ContractSave(id, type, null, null, 0, "", "", 0, false, broken = true)
        } catch (_: BufferUnderflowException) {
            ContractSave(id, type, null, null, 0, "", "", 0, false, broken = true)
        }
    }

    private fun readBody(
        r: PacketReader,
        id: Int,
        type: Int,
    ): ContractSave {
        val give = if (r.boolean()) List(r.countInt(Int.SIZE_BYTES)) { rule(r) } else null
        val get = if (r.boolean()) rule(r) else null
        return when (type) {
            ContractKind.PAYMENT.wire -> payment(r, id, type, give, get)
            ContractKind.REWARD.wire -> reward(r, id, type, give, get)
            else -> ContractSave(id, type, give, get, 0, "", "", 0, false, broken = false)
        }
    }

    private fun payment(
        r: PacketReader,
        id: Int,
        type: Int,
        give: List<List<Term>>?,
        get: List<Term>?,
    ): ContractSave {
        val mode = r.short().toInt()
        if (mode !in 0..KNOWN_MODES) return ContractSave(id, type, give, get, mode, "", "", 0, false, broken = true)
        return ContractSave(id, type, give, get, mode, r.string(), r.string(), 0, false, broken = false)
    }

    private fun reward(
        r: PacketReader,
        id: Int,
        type: Int,
        give: List<List<Term>>?,
        get: List<Term>?,
    ): ContractSave {
        val category = r.short().toInt()
        val popup = r.boolean()
        return ContractSave(id, type, give, get, 0, r.string(), "", category, popup, broken = false)
    }

    private fun rule(r: PacketReader): List<Term> = List(r.countInt(1)) { node(r) }

    private fun node(r: PacketReader): Term =
        when (r.byte().toInt()) {
            NODE_CREDITS -> {
                Term(credits = true, amount = r.int())
            }

            NODE_FURNI -> {
                Term(
                    credits = false,
                    amount = r.int(),
                    wall = r.boolean(),
                    typeId = r.int(),
                    extra = r.string(),
                )
            }

            else -> {
                throw BrokenRule()
            }
        }

    override fun write(
        w: PacketWriter,
        m: ContractSave,
    ) {
        w.int(m.id).short(m.type.toShort())
        w.boolean(m.give != null)
        m.give?.let { rules ->
            w.int(rules.size)
            rules.forEach { ContractWire.rule(w, it) }
        }
        w.boolean(m.get != null)
        m.get?.let { ContractWire.rule(w, it) }
    }
}
