package zel.room.furni.place

enum class PlaceError(
    val key: String,
) {
    CantSetItem("cant_set_item"),
    NotOwner("cant_set_not_owner"),
    MaxFurniture("max_furniture"),
    MaxDimmers("max_dimmers"),
    MaxSoundFurni("max_soundfurni"),
    TypeLimit("type_limit"),
    MaxStickies("max_stickies"),
    ;

    val message: String get() = "\${room.error.$key}"
}
