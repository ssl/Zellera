package zel.wired.variable.addon

import zel.protocol.generated.VariableFxConfigsOut
import zel.wired.box.BoxSettings

data class FxConfig(
    val id: Int,
    val forUsers: Boolean,
    val showMode: Int,
    val updateMask: Int,
    val showOnHover: Boolean,
    val showMillis: Int,
    val kind: FxKind,
    val style: Int,
    val color: Int,
    val width: Int,
    val renderer: Int,
    val min: Long,
    val max: Long,
    val extra: List<Pair<String, String>>,
) {
    fun wire(): VariableFxConfigsOut.List1Item =
        VariableFxConfigsOut.List1Item(
            id,
            forUsers,
            showMode,
            updateMask,
            showOnHover,
            showMillis,
            kind.category,
            style,
            color,
            width,
            renderer,
            min,
            max,
            extra.map { VariableFxConfigsOut.List1Item.List1Item(it.first, it.second) },
        )

    companion object {
        fun of(
            id: Int,
            kind: FxKind,
            ints: List<Int>,
        ): FxConfig {
            val form = FxForm(ints)
            val styleId = kind.style(form.style)
            val style = kind.styles[styleId]
            val min = if (kind == FxKind.LEVELLING) 0L else form.min
            val max = if (kind == FxKind.LEVELLING) FxForm.DEFAULT_MAX else form.max
            return FxConfig(
                id,
                form.forUsers,
                form.showMode,
                form.updateMask,
                form.showOnHover,
                form.showMillis,
                kind,
                styleId,
                form.color(kind),
                form.width(kind),
                style.renderer,
                min,
                roomyMax(min, max),
                extra(kind, styleId, style, form),
            )
        }

        private fun roomyMax(
            min: Long,
            max: Long,
        ): Long =
            when {
                max > min -> max
                min < FxForm.DEFAULT_MAX -> FxForm.DEFAULT_MAX
                else -> min + 1
            }

        private fun extra(
            kind: FxKind,
            styleId: Int,
            style: FxStyle,
            form: FxForm,
        ): List<Pair<String, String>> {
            val sub = form.subRenderer
            if (kind == FxKind.LEVELLING && styleId == 0 && sub == PICKED_SUB_RENDERER) {
                return listOf(FxForm.SUB_RENDERER to sub.toString())
            }
            val segments = form.segments.coerceAtMost(MAX_SEGMENTS)
            return if (style.renderer in SEGMENTED_RENDERERS && segments > 0) {
                style.extra + (FxForm.SEGMENTS to segments.toString())
            } else {
                style.extra
            }
        }

        private const val PICKED_SUB_RENDERER = 3
        private const val MAX_SEGMENTS = 100
        private const val RENDERER_STATUS = 2
        private const val RENDERER_PROGRESS_SEGMENTED = 4
        private const val RENDERER_HEALTH_SEGMENTED = 13
        private val SEGMENTED_RENDERERS =
            setOf(RENDERER_STATUS, RENDERER_PROGRESS_SEGMENTED, RENDERER_HEALTH_SEGMENTED)

        fun filled(
            kind: FxKind,
            settings: BoxSettings,
        ): BoxSettings {
            val ints =
                settings.ints.mapIndexed { i, value ->
                    when {
                        kind == FxKind.LEVELLING && i in FxForm.RANGE_INTS -> FxForm.UNSET
                        value != FxForm.UNSET -> value
                        i == FxForm.COLOR -> kind.color
                        i == FxForm.WIDTH -> kind.width
                        i == FxForm.RENDERER -> kind.renderer
                        else -> value
                    }
                }
            return settings.copy(ints = ints)
        }
    }
}
