package zel.wired.store

import com.google.gson.JsonObject
import zel.wired.box.BoxSettings

data class StoredBox(
    val settings: BoxSettings,
    val isFresh: Boolean,
    val extras: JsonObject? = null,
)
