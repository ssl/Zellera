package zel.wired.variable.addon

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.BuildPass
import zel.room.furni.place.Interactions
import zel.wired.BoxSettingsSource
import zel.wired.firing.HeldValue
import zel.wired.variable.ChangeOrigin
import zel.wired.variable.Holder
import zel.wired.variable.RoomVariables
import zel.wired.variable.Variable
import zel.wired.variable.VariableOn

@Suppress("ThrowsCount") // a refusal is the call's answer, one line per rule of the contract
class BridgeOps(
    private val variables: () -> RoomVariables,
    private val names: ApiVariables,
    private val forms: BoxSettingsSource,
    /** A key works only while its owner could save the room's wired. */
    private val mayModify: (Room, Int) -> Boolean,
) {
    fun run(
        scope: RoomScope,
        op: String,
        args: JsonObject,
    ): JsonElement {
        checkBoxes(scope.room, args)
        return when (op) {
            "listVariables" -> listVariables(scope.room)
            "getEntry" -> getEntry(scope, args)
            "putEntry" -> putEntry(scope, args)
            "patchEntry" -> patchEntry(scope, args)
            "deleteEntry" -> deleteEntry(scope, args)
            "listHolders" -> listHolders(scope, args)
            "countHolders" -> countHolders(scope, args)
            "bulkDelete" -> bulkDelete(scope, args)
            "batch" -> batch(scope, args)
            "getGlobal" -> getGlobal(scope, args)
            "patchGlobal" -> patchGlobal(scope, args)
            "getProfile" -> getProfile(scope, args)
            "patchProfile" -> patchProfile(scope, args)
            "deleteProfile" -> deleteProfile(scope, args)
            "getGlobalProfile" -> globalProfile(scope)
            "patchGlobalProfile" -> patchGlobalProfile(scope, args)
            else -> throw BridgeRefusal(BAD_REQUEST, "wired.variables.invalid_target")
        }
    }

    private fun checkBoxes(
        room: Room,
        args: JsonObject,
    ) {
        val boxes = args.getAsJsonObject("boxes") ?: return
        boxes.entrySet().forEach { (_, id) ->
            val box = room.furni[id.asInt]?.takeIf { Interactions.of(it.definition) == AddonCodes.WEB_API }
            val allowed =
                box != null && (box.ownerId == room.info.ownerId || mayModify(room, box.ownerId))
            if (!allowed) throw BridgeRefusal(FORBIDDEN, API_DISABLED)
        }
    }

    private fun listVariables(room: Room): JsonElement {
        val api = names.of(room)

        fun list(names: Map<String, Int>) = JsonArray().also { a -> names.keys.forEach(a::add) }
        return JsonObject().apply {
            add("users", list(api.users))
            add("furni", list(api.furni))
            add("global", list(api.global))
        }
    }

    private fun scopeNames(
        room: Room,
        scope: String,
    ): Map<String, Int> {
        val api = names.of(room)
        return when (scope) {
            USER -> api.users
            FURNI -> api.furni
            else -> api.global
        }
    }

    private fun variable(
        room: Room,
        scope: String,
        name: String,
        refusal: BridgeRefusal = NOT_FOUND,
    ): Variable {
        val boxId = scopeNames(room, scope)[name] ?: throw refusal
        val box = room.furni[boxId] ?: throw refusal
        return variables().definition(room, box) ?: throw refusal
    }

    private fun variable(
        room: Room,
        args: JsonObject,
    ): Variable = variable(room, args.get("scope").asString, args.get("name").asString)

    private fun holder(
        room: Room,
        kind: String,
        id: Int,
    ): Holder? =
        when (kind) {
            USERS -> {
                Holder.Player(id)
            }

            "bots" -> {
                Holder.Bot(id).takeIf { inRoom(room, it) }
            }

            "pets" -> {
                Holder.Pet(id).takeIf { inRoom(room, it) }
            }

            in PLAIN_FURNI -> {
                Holder.Furni(id).takeIf { room.furni[id] != null }
            }

            in BC_FURNI -> {
                val furniId = id + BuildPass.FIRST_ID
                Holder.Furni(furniId).takeIf { room.furni[furniId] != null }
            }

            else -> {
                null
            }
        }

    private fun inRoom(
        room: Room,
        holder: Holder,
    ) = room.units.all().any { Holder.of(it) == holder }

    private fun target(
        room: Room,
        args: JsonObject,
    ): Holder? = holder(room, args.get("kind").asString, args.get("id").asInt)

    private fun value(element: JsonElement?): Long {
        val text = element?.takeIf { it.isJsonPrimitive }?.asString
        return text?.toLongOrNull() ?: throw BridgeRefusal(BAD_REQUEST, "wired.variables.invalid_value")
    }

    private fun entry(
        variable: Variable,
        held: HeldValue,
    ): JsonObject =
        JsonObject().apply {
            if (variable.entry.flags.hasValue) {
                addProperty("value", held.value.toString())
            } else {
                add("value", JsonNull.INSTANCE)
            }
            addProperty("createdAt", held.created)
            addProperty("updatedAt", held.updated)
        }

    private fun getEntry(
        scope: RoomScope,
        args: JsonObject,
    ): JsonElement {
        val variable = variable(scope.room, args)
        val holder = target(scope.room, args) ?: throw NOT_FOUND
        val held = variable.read(VariableOn(scope), holder) ?: throw NOT_FOUND
        return entry(variable, held)
    }

    private fun putEntry(
        scope: RoomScope,
        args: JsonObject,
    ): JsonElement {
        val value = value(args.get("value"))
        val variable = variable(scope.room, args)
        val holder = target(scope.room, args) ?: throw NOT_FOUND
        return put(scope, variable, holder, value)
    }

    private fun put(
        scope: RoomScope,
        variable: Variable,
        holder: Holder,
        value: Long,
    ): JsonElement {
        if (holder is Holder.Player && !participates(scope.room, holder)) {
            throw BridgeRefusal(FORBIDDEN, NOT_PARTICIPATING)
        }
        val on = VariableOn(scope)
        if (!variable.give(on, holder, value, true, ChangeOrigin.EXTERNAL)) throw NOT_ALLOWED
        return entry(variable, variable.read(on, holder) ?: throw NOT_FOUND)
    }

    /** The room holds every stored value from its load on, so what it has in memory is the whole answer. */
    private fun participates(
        room: Room,
        holder: Holder,
    ): Boolean {
        val all = variables()
        return names.of(room).users.values.any { boxId ->
            val box = room.furni[boxId] ?: return@any false
            val variable = all.definition(room, box) ?: return@any false
            all.holds(room, variable, holder)
        }
    }

    private fun patchEntry(
        scope: RoomScope,
        args: JsonObject,
    ): JsonElement {
        val value = value(args.get("value"))
        val variable = variable(scope.room, args)
        val holder = target(scope.room, args) ?: throw NOT_FOUND
        return patch(scope, variable, holder, value)
    }

    private fun patch(
        scope: RoomScope,
        variable: Variable,
        holder: Holder,
        value: Long,
    ): JsonElement {
        val on = VariableOn(scope)
        variable.read(on, holder) ?: throw NOT_FOUND
        if (!variable.write(on, holder, value, ChangeOrigin.EXTERNAL)) throw NOT_ALLOWED
        return entry(variable, variable.read(on, holder) ?: throw NOT_FOUND)
    }

    private fun deleteEntry(
        scope: RoomScope,
        args: JsonObject,
    ): JsonElement {
        val variable = variable(scope.room, args)
        val holder = target(scope.room, args) ?: throw NOT_FOUND
        if (!variable.remove(VariableOn(scope), holder, ChangeOrigin.EXTERNAL)) throw NOT_FOUND
        return JsonNull.INSTANCE
    }

    private fun holdersOf(
        room: Room,
        variable: Variable,
        kind: String,
    ): List<Pair<Int, Map.Entry<Holder, HeldValue>>> =
        variables().allHolders(room, variable).entries.mapNotNull { held ->
            idInKind(held.key, kind)?.let { it to held }
        }

    private fun idInKind(
        holder: Holder,
        kind: String,
    ): Int? =
        when {
            holder is Holder.Player && kind == USERS -> {
                holder.id
            }

            holder is Holder.Bot && kind == "bots" -> {
                holder.id
            }

            holder is Holder.Pet && kind == "pets" -> {
                holder.id
            }

            holder is Holder.Furni && kind in PLAIN_FURNI && !BuildPass.isClientId(holder.id) -> {
                holder.id
            }

            holder is Holder.Furni && kind in BC_FURNI && BuildPass.isClientId(holder.id) -> {
                holder.id - BuildPass.FIRST_ID
            }

            else -> {
                null
            }
        }

    private fun listHolders(
        scope: RoomScope,
        args: JsonObject,
    ): JsonElement {
        val room = scope.room
        val variable = variable(room, args)
        val orderBy = args.get("orderBy")?.asString ?: "creation_time"
        val descending = args.get("orderDir")?.asString == "desc"
        val page = args.get("page")?.asInt ?: 1
        val size = args.get("size")?.asInt ?: DEFAULT_PAGE
        val sort: Comparator<Pair<Int, Map.Entry<Holder, HeldValue>>> =
            compareBy {
                when (orderBy) {
                    "value" -> it.second.value.value
                    "update_time" -> it.second.value.updated
                    else -> it.second.value.created
                }
            }
        val order = if (descending) sort.reversed() else sort
        val sorted = holdersOf(room, variable, args.get("kind").asString).sortedWith(order)
        val shown = sorted.drop((page - 1) * size).take(size)
        val userNames = variables().names(shown.mapNotNull { (it.second.key as? Holder.Player)?.id })
        val holders = JsonArray()
        shown.forEach { (id, held) ->
            holders.add(
                JsonObject().apply {
                    addProperty("id", id)
                    (held.key as? Holder.Player)?.let { userNames[it.id] }?.let { addProperty("name", it) }
                    add("entry", entry(variable, held.value))
                },
            )
        }
        return JsonObject().apply {
            add("holders", holders)
            addProperty("page", page)
            addProperty("size", size)
        }
    }

    private fun countHolders(
        scope: RoomScope,
        args: JsonObject,
    ): JsonElement {
        val variable = variable(scope.room, args)
        return JsonObject().apply {
            addProperty("count", holdersOf(scope.room, variable, args.get("kind").asString).size)
        }
    }

    private fun bulkDelete(
        scope: RoomScope,
        args: JsonObject,
    ): JsonElement {
        val room = scope.room
        val boxId = args.getAsJsonObject("boxes")?.get("write")?.asInt
        val box = boxId?.let { room.furni[it] }
        val enabled = box?.let { forms.settings(room, it)?.ints?.getOrNull(0) } == 1
        if (!enabled) throw BridgeRefusal(FORBIDDEN, "wired.variables.bulk_delete_not_enabled")
        val invalid = BridgeRefusal(BAD_REQUEST, "wired.variables.bulk_delete_invalid_variable")
        val api = names.of(room)
        val chosen =
            args.getAsJsonArray("names").map { name ->
                val scopeName = if (name.asString in api.users) USER else FURNI
                variable(room, scopeName, name.asString, invalid)
            }
        val on = VariableOn(scope)
        chosen.forEach { variable ->
            variables().allHolders(room, variable).keys.toList().forEach {
                variable.remove(on, it, ChangeOrigin.EXTERNAL)
            }
        }
        return JsonNull.INSTANCE
    }

    private fun batch(
        scope: RoomScope,
        args: JsonObject,
    ): JsonElement {
        val room = scope.room
        val variable = variable(room, args.get("scope").asString, args.get("name").asString)
        val results = JsonArray()
        variables().atOneTime {
            args.getAsJsonArray("ops").forEach { results.add(batchOne(scope, variable, it.asJsonObject)) }
        }
        return JsonObject().apply { add("results", results) }
    }

    private fun batchOne(
        scope: RoomScope,
        variable: Variable,
        op: JsonObject,
    ): JsonObject {
        val result = JsonObject()
        result.addProperty("i", op.get("i").asInt)
        try {
            val holder = holder(scope.room, op.get("kind").asString, op.get("id").asInt) ?: throw NOT_FOUND
            val on = VariableOn(scope)
            when (op.get("method").asString) {
                "GET" -> {
                    result.add("entry", entry(variable, variable.read(on, holder) ?: throw NOT_FOUND))
                    result.addProperty("status", OK)
                }

                "PUT" -> {
                    result.add("entry", put(scope, variable, holder, value(op.get("value"))))
                    result.addProperty("status", OK)
                }

                "PATCH" -> {
                    result.add("entry", patch(scope, variable, holder, value(op.get("value"))))
                    result.addProperty("status", OK)
                }

                else -> {
                    if (!variable.remove(on, holder, ChangeOrigin.EXTERNAL)) throw NOT_FOUND
                    result.addProperty("status", NO_CONTENT)
                }
            }
        } catch (refused: BridgeRefusal) {
            result.addProperty("status", refused.status)
            result.addProperty("error", refused.code)
        }
        return result
    }

    private fun getGlobal(
        scope: RoomScope,
        args: JsonObject,
    ): JsonElement {
        val variable = variable(scope.room, GLOBAL, args.get("name").asString)
        return entry(variable, variable.read(VariableOn(scope), Holder.Room) ?: throw NOT_FOUND)
    }

    private fun patchGlobal(
        scope: RoomScope,
        args: JsonObject,
    ): JsonElement {
        val value = value(args.get("value"))
        val variable = variable(scope.room, GLOBAL, args.get("name").asString)
        return patch(scope, variable, Holder.Room, value)
    }

    private fun held(
        scope: RoomScope,
        scopeName: String,
        holder: Holder,
    ): JsonObject {
        val on = VariableOn(scope)
        val result = JsonObject()
        scopeNames(scope.room, scopeName).keys.forEach { name ->
            val variable = variable(scope.room, scopeName, name)
            variable.read(on, holder)?.let { result.add(name, entry(variable, it)) }
        }
        return result
    }

    private fun profile(
        scope: RoomScope,
        scopeName: String,
        kind: String,
        id: Int,
    ): JsonElement {
        val holder = holder(scope.room, kind, id) ?: throw ENTITY_NOT_FOUND
        val values = held(scope, scopeName, holder)
        if (scopeName == USER && kind == USERS && values.size() == 0) throw ENTITY_NOT_FOUND
        return JsonObject().apply {
            add(
                "target",
                JsonObject().apply {
                    addProperty("id", id)
                    if (holder is Holder.Player) variables().names(listOf(id))[id]?.let { addProperty("name", it) }
                },
            )
            add("variables", values)
        }
    }

    private fun getProfile(
        scope: RoomScope,
        args: JsonObject,
    ): JsonElement = profile(scope, args.get("scope").asString, args.get("kind").asString, args.get("id").asInt)

    private fun patchProfile(
        scope: RoomScope,
        args: JsonObject,
    ): JsonElement {
        val room = scope.room
        val scopeName = args.get("scope").asString
        val kind = args.get("kind").asString
        val id = args.get("id").asInt
        val holder = holder(room, kind, id) ?: throw ENTITY_NOT_FOUND
        val invalid = BridgeRefusal(BAD_REQUEST, INVALID_TARGET)
        val changes =
            args.getAsJsonObject("variables").entrySet().map { (name, value) ->
                variable(room, scopeName, name, invalid) to value
            }
        if (holder is Holder.Player && !participates(room, holder)) throw ENTITY_NOT_FOUND
        val on = VariableOn(scope)
        changes.forEach { (variable, value) ->
            when {
                value.isJsonNull -> {
                    variable.remove(on, holder, ChangeOrigin.EXTERNAL)
                }

                variable.read(on, holder) == null -> {
                    variable.give(on, holder, value(value), true, ChangeOrigin.EXTERNAL)
                }

                else -> {
                    variable.write(on, holder, value(value), ChangeOrigin.EXTERNAL)
                }
            }
        }
        return profile(scope, scopeName, kind, id)
    }

    private fun deleteProfile(
        scope: RoomScope,
        args: JsonObject,
    ): JsonElement {
        val room = scope.room
        val holder = holder(room, args.get("kind").asString, args.get("id").asInt) ?: throw ENTITY_NOT_FOUND
        val on = VariableOn(scope)
        val removed =
            names.of(room).users.keys.count { name ->
                variable(room, USER, name).remove(on, holder, ChangeOrigin.EXTERNAL)
            }
        if (removed == 0) throw ENTITY_NOT_FOUND
        return JsonNull.INSTANCE
    }

    private fun globalProfile(scope: RoomScope): JsonElement =
        JsonObject().apply { add("variables", held(scope, GLOBAL, Holder.Room)) }

    private fun patchGlobalProfile(
        scope: RoomScope,
        args: JsonObject,
    ): JsonElement {
        val room = scope.room
        val entries = args.getAsJsonObject("variables").entrySet()
        if (entries.any { it.value.isJsonNull }) throw NOT_ALLOWED
        val invalid = BridgeRefusal(BAD_REQUEST, INVALID_TARGET)
        val changes = entries.map { (name, value) -> variable(room, GLOBAL, name, invalid) to value(value) }
        val on = VariableOn(scope)
        changes.forEach { (variable, value) -> variable.write(on, Holder.Room, value, ChangeOrigin.EXTERNAL) }
        return globalProfile(scope)
    }

    private companion object {
        const val USER = "user"
        const val FURNI = "furni"
        const val GLOBAL = "global"
        const val USERS = "users"
        const val OK = 200
        const val NO_CONTENT = 204
        const val BAD_REQUEST = 400
        const val FORBIDDEN = 403
        const val MISSING = 404
        const val DEFAULT_PAGE = 50
        val PLAIN_FURNI = setOf(FURNI, "wall-items")
        val BC_FURNI = setOf("furni-bc", "wall-items-bc")
        const val API_DISABLED = "wired.variables.api_disabled"
        const val NOT_PARTICIPATING = "wired.variables.user_not_participating"
        const val INVALID_TARGET = "wired.variables.invalid_target"
        val NOT_FOUND = BridgeRefusal(MISSING, "wired.variables.not_found")
        val ENTITY_NOT_FOUND = BridgeRefusal(MISSING, "wired.variables.entity_not_found")
        val NOT_ALLOWED = BridgeRefusal(FORBIDDEN, "wired.variables.operation_not_allowed")
    }
}
