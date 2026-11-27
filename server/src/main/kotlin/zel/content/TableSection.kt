package zel.content

internal data class TableSection(
    val id: String,
    val table: String,
    val mode: ImportMode = ImportMode.REPLACE,
    val keep: String? = null,
    val skip: String? = null,
    val roomScope: String? = null,
    val retired: Set<String> = emptySet(),
    val owns: IdSpace? = null,
    val refs: Map<String, IdSpace> = emptyMap(),
    val textRefs: Set<String> = emptySet(),
)
