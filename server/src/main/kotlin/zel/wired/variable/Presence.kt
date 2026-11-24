package zel.wired.variable

internal object Presence {
    fun present(
        on: VariableOn,
        holders: Collection<Holder>,
    ): List<Holder> {
        val room = on.scope.room
        val units =
            room.units
                .all()
                .map { Holder.of(it) }
                .toSet()
        return holders.filter { holder ->
            when (holder) {
                is Holder.Furni -> room.furni[holder.id] != null
                Holder.Room, Holder.Context -> true
                else -> holder in units
            }
        }
    }
}
