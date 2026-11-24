package zel.wired.box

class BoxCatalog(
    types: List<BoxType<WiredBox>>,
) {
    private val byCode: Map<String, BoxType<WiredBox>>

    init {
        val dup = types.groupBy { it.code }.filterValues { it.size > 1 }.keys
        require(dup.isEmpty()) { "wired box codes registered twice: $dup" }
        byCode = types.associateBy { it.code }
    }

    operator fun get(code: String): BoxType<WiredBox>? = byCode[code]

    fun codes(): Set<String> = byCode.keys
}
