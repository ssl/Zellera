package zel.protocol.navigator

/** [categoryId] 0 means the personal space category. */
data class NewRoom(
    val name: String,
    val description: String,
    val modelName: String,
    val categoryId: Int,
    val maxUsers: Int,
    val tradeMode: Int,
)
