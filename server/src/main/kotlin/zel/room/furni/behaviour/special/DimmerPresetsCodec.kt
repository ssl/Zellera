package zel.room.furni.behaviour.special

import zel.protocol.generated.OutgoingCodec
import zel.protocol.generated.PacketReader
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.RoomDimmerPresetsOutCodec

object DimmerPresetsCodec : OutgoingCodec<DimmerPresetsCodec.Presets> {
    override val name = RoomDimmerPresetsOutCodec.name

    data class Presets(
        val selected: Int,
        val presets: List<Dimmers.Preset>,
        val on: Boolean,
        val furniId: Int,
    )

    override fun read(r: PacketReader): Presets {
        val count = r.int()
        val selected = r.int()
        val presets = List(count) { Dimmers.Preset(r.int(), r.int() == 2, r.string(), r.int()) }
        return Presets(selected, presets, r.boolean(), r.int())
    }

    override fun write(
        w: PacketWriter,
        m: Presets,
    ) {
        w.int(m.presets.size).int(m.selected)
        m.presets.forEach {
            w
                .int(it.id)
                .int(it.type)
                .string(it.colour)
                .int(it.brightness)
        }
        w.boolean(m.on).int(m.furniId)
    }
}
