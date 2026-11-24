package zel.wired.addon.label

interface LabelStore {
    fun load(furniIds: Collection<Int>): Map<Int, Label>

    fun save(
        furniId: Int,
        label: Label,
    )

    fun delete(furniId: Int)
}
