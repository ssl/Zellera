package zel.wired.chest

import zel.protocol.generated.IncomingCodec
import zel.protocol.generated.PacketReader
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.SetChestNotificationSettingsIn
import zel.protocol.generated.SetChestNotificationSettingsInCodec

/** The wire order of SetChestNotificationSettings: the mode comes before the switches. */
object ChestNotificationsCodec : IncomingCodec<SetChestNotificationSettingsIn> {
    override val name = SetChestNotificationSettingsInCodec.name

    override fun read(r: PacketReader): SetChestNotificationSettingsIn {
        val id = r.int()
        val mode = r.int()
        return SetChestNotificationSettingsIn(
            id,
            r.boolean(),
            r.boolean(),
            r.boolean(),
            r.boolean(),
            r.boolean(),
            mode,
        )
    }

    override fun write(
        w: PacketWriter,
        m: SetChestNotificationSettingsIn,
    ) {
        w
            .int(m.itemId)
            .int(m.mode)
            .boolean(m.full)
            .boolean(m.donation)
            .boolean(m.withdraw)
        w.boolean(m.empty).boolean(m.wired)
    }
}
