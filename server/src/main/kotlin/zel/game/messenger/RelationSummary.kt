package zel.game.messenger

/** One relation of a profile: how many friends carry it and the one shown. */
data class RelationSummary(
    val relation: Int,
    val friends: Int,
    val shownId: Int,
    val shownName: String,
    val shownLook: String,
) {
    companion object {
        /** A hidden profile shows its relations to nobody but its owner; [read] is not asked for the others. */
        fun shownTo(
            viewerId: Int,
            ownerId: Int,
            hidden: Boolean,
            read: () -> List<RelationSummary>,
        ): List<RelationSummary> = if (hidden && viewerId != ownerId) emptyList() else read()

        fun of(friends: Collection<Friend>): List<RelationSummary> =
            friends
                .filter { it.relation > 0 }
                .groupBy { it.relation.toInt() }
                .toSortedMap()
                .map { (relation, carrying) ->
                    val shown = carrying.random()
                    RelationSummary(relation, carrying.size, shown.id, shown.username, shown.look)
                }
    }
}
