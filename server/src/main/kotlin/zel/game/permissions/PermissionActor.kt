package zel.game.permissions

data class PermissionActor(
    val id: Int,
    val name: String?,
    val via: String?,
) {
    companion object {
        const val SYSTEM_ID = 0
    }
}
