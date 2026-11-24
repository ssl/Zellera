package zel.wired.variable

data class VariableFlags(
    val alwaysAvailable: Boolean,
    val canCreateAndDelete: Boolean,
    val hasValue: Boolean,
    val canWriteValue: Boolean,
    val canInterceptChanges: Boolean,
    val isInvisible: Boolean,
    val requiresExecutionContext: Boolean,
    val canReadCreationTime: Boolean,
    val canReadLastUpdateTime: Boolean,
) {
    fun all(): List<Boolean> =
        listOf(
            alwaysAvailable,
            canCreateAndDelete,
            hasValue,
            canWriteValue,
            canInterceptChanges,
            isInvisible,
            requiresExecutionContext,
            canReadCreationTime,
            canReadLastUpdateTime,
        )

    companion object {
        private const val COUNT = 9

        fun of(bits: String): VariableFlags {
            require(bits.length == COUNT) { "variable flags $bits" }
            val on = bits.map { it == '1' }
            var index = 0
            return VariableFlags(
                alwaysAvailable = on[index++],
                canCreateAndDelete = on[index++],
                hasValue = on[index++],
                canWriteValue = on[index++],
                canInterceptChanges = on[index++],
                isInvisible = on[index++],
                requiresExecutionContext = on[index++],
                canReadCreationTime = on[index++],
                canReadLastUpdateTime = on[index],
            )
        }
    }
}
