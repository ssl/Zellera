package zel.wired.variable.addon

import zel.wired.box.BoxSettings
import zel.wired.firing.Firing
import zel.wired.trigger.LineCapture
import zel.wired.variable.Holder
import zel.wired.variable.RoomVariables
import zel.wired.variable.Variable
import zel.wired.variable.VariableOn

class TextInputVariable(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val variables: RoomVariables,
) : LineCapture {
    val name: String get() = settings.text

    private val asText = settings.ints.getOrElse(0) { 0 } != 0

    override fun capturesIn(pattern: String): Boolean = pattern.contains("#($name)", ignoreCase = true)

    override fun capture(
        firing: Firing,
        pattern: String,
        line: String,
        contains: Boolean,
    ): Boolean {
        val variable =
            variables.find(firing.scope.room, settings.variableIds.firstOrNull().orEmpty()) ?: return false
        // A number starts right after the text before the marker: ":cash#(n)" refuses ":cash 10".
        val inputs = firing.addons.filterIsInstance<TextInputVariable>().mapTo(HashSet()) { it.name }
        val value =
            (
                if (asText) {
                    TextCapture.part(pattern, name, line, contains, inputs)?.let { valueOf(variable, it) }
                } else {
                    TextCapture.number(pattern, name, line, contains, inputs)
                }
            ) ?: return false
        return variable.give(VariableOn(firing.scope, firing), Holder.Context, value, true, 0)
    }

    // With a text connector only its texts are taken, case and trailing spaces aside: with '1=north\n2=south' it sets 2
    // for 'go south' and 'go South ', and leaves 'go 2' and 'go  south now' unanswered.
    private fun valueOf(
        variable: Variable,
        text: String,
    ): Long? {
        val texts = variable.entry.texts ?: return text.trim().toLongOrNull()
        return texts.entries
            .firstOrNull { it.second.equals(text.trimEnd(), ignoreCase = true) }
            ?.first
            ?.toLong()
    }
}
