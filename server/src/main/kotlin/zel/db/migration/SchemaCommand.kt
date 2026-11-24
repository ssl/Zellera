package zel.db.migration

/** What the command line asks of the schema: without arguments the server checks it and serves. */
data class SchemaCommand(
    val step: Step,
    val serves: Boolean,
) {
    enum class Step(
        val word: String,
    ) {
        APPLY("apply"),
        CHECK("check"),
        REPAIR("repair"),
    }

    companion object {
        const val NAME = "schema"
        val SERVE = SchemaCommand(Step.CHECK, serves = true)

        /** Anything but no arguments or `schema <step>` is refused, so a slip of the keys never starts a game. */
        fun fromCommandLine(args: Array<String>): SchemaCommand {
            if (args.isEmpty()) return SERVE
            val step = Step.entries.firstOrNull { args.size == 2 && args[0] == NAME && args[1] == it.word }
            return SchemaCommand(requireNotNull(step) { usage(args) }, serves = false)
        }

        private fun usage(args: Array<String>): String =
            "not understood: ${args.joinToString(" ")}\n" +
                "start the server without arguments, or run one schema step and end: " +
                Step.entries.joinToString(", ") { "$NAME ${it.word}" }
    }
}
