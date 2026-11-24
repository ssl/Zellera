package zel.wired.variable.addon

/** Called on the persistence executor. */
interface ImportStore {
    fun importsOf(
        roomId: Int,
        boxId: Int,
    ): List<ImportRow>

    fun importsIn(roomId: Int): List<ImportRow>

    fun sharedBy(token: String): ImportedAddons?

    fun save(
        row: ImportRow,
        addons: ImportedAddons,
    )

    companion object {
        fun tokenOf(
            roomId: Int,
            boxId: Int,
        ): String = "$roomId:$boxId"
    }
}
