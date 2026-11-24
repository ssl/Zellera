package zel.wired.box

class BoxType<out B : WiredBox>(
    val code: String,
    val kind: BoxKind,
    val form: BoxForm,
    val create: (furniId: Int, settings: BoxSettings) -> B,
)
