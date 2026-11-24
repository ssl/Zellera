package zel.protocol.snowball

@JvmRecord
data class Game2PlayerData(
    val referenceId: Int,
    val name: String,
    val figure: String,
    val gender: String,
    val teamId: Int,
)
