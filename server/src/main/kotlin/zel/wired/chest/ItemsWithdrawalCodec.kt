package zel.wired.chest

import zel.protocol.generated.IncomingCodec
import zel.protocol.generated.PacketReader
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.WithdrawItemsFromChestInCodec

/** The full layout of WithdrawItemsFromChest; the generated codec reads only its first two ints. */
object ItemsWithdrawalCodec : IncomingCodec<ItemsWithdrawal> {
    override val name = WithdrawItemsFromChestInCodec.name

    override fun read(r: PacketReader): ItemsWithdrawal =
        ItemsWithdrawal(r.int(), r.boolean(), r.int(), r.string(), r.int())

    override fun write(
        w: PacketWriter,
        m: ItemsWithdrawal,
    ) {
        w
            .int(m.chestId)
            .boolean(m.wall)
            .int(m.typeId)
            .string(m.extra)
            .int(m.quantity)
    }
}
