package zel.wired.firing

import zel.protocol.generated.PacketWriter
import zel.protocol.generated.WiredMovementsOut

object MovementWire {
    fun message(entries: List<Movement>): WiredMovementsOut {
        val w = PacketWriter()
        w.countInt(entries.size)
        entries.forEach { write(w, it) }
        return WiredMovementsOut(w.toByteArray())
    }

    private fun write(
        w: PacketWriter,
        m: Movement,
    ) {
        when (m) {
            is Movement.UnitMove -> {
                w.int(Movement.TYPE_UNIT)
                spots(w, m.from, m.to)
                w
                    .int(m.unitIndex)
                    .int(m.moveType)
                    .int(m.duration)
                    .int(m.bodyDirection)
                    .int(m.headDirection)
                w.boolean(false)
            }

            is Movement.FurniMove -> {
                w.int(Movement.TYPE_FURNI)
                spots(w, m.from, m.to)
                w.int(m.furniId).int(m.duration).int(m.rotation)
                w.boolean(false).boolean(false)
            }

            is Movement.WallMove -> {
                w.int(Movement.TYPE_WALL).int(m.furniId).boolean(m.rightSide)
                wall(w, m.from)
                wall(w, m.to)
                w.int(m.duration)
            }

            is Movement.UnitTurn -> {
                w
                    .int(Movement.TYPE_TURN)
                    .int(m.unitIndex)
                    .int(m.direction)
                    .int(m.direction)
            }
        }
    }

    private fun spots(
        w: PacketWriter,
        from: Movement.Spot,
        to: Movement.Spot,
    ) {
        w
            .int(from.x)
            .int(from.y)
            .int(to.x)
            .int(to.y)
            .string(from.z)
            .string(to.z)
    }

    private fun wall(
        w: PacketWriter,
        spot: Movement.WallSpot,
    ) {
        w
            .int(spot.wallX)
            .int(spot.wallY)
            .int(spot.localX)
            .int(spot.localY)
    }
}
