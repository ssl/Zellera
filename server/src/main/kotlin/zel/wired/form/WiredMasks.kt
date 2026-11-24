package zel.wired.form

data class WiredMasks(
    val modify: Int,
    val read: Int,
    val timezone: String = "",
) {
    fun mayModify(level: Int): Boolean = passes(modify and EVERYONE.inv(), level)

    fun mayRead(level: Int): Boolean = passes(read, level)

    private fun passes(
        mask: Int,
        level: Int,
    ): Boolean =
        mask and EVERYONE != 0 ||
            when (level) {
                OWNER, STAFF -> true
                GROUP_ADMIN -> mask and (GROUP_ADMINS or GROUP_MEMBERS) != 0
                GROUP_MEMBER -> mask and GROUP_MEMBERS != 0
                RIGHTS -> mask and RIGHTS_HOLDERS != 0
                else -> false
            }

    companion object {
        const val EVERYONE = 1
        const val RIGHTS_HOLDERS = 2
        const val GROUP_MEMBERS = 4
        const val GROUP_ADMINS = 8

        private const val RIGHTS = 1
        private const val GROUP_MEMBER = 2
        private const val GROUP_ADMIN = 3
        private const val OWNER = 4
        private const val STAFF = 5

        val FRESH = WiredMasks(GROUP_ADMINS, GROUP_ADMINS)
    }
}
