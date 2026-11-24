# Game extensions

A game extension is a jar that runs inside the game server's rooms. It can listen to everything that happens in a
room, act every room tick, take over walk clicks, choose where an entering player appears, put furni into a room
that lives only as long as the room is loaded, slide furni and avatars together, and keep numbers per player in the
database. Use it for game modes that need more than wired: vehicles, role-play rules, whole games.

Web extensions (pages, profile parts, housekeeping) are something else: see web/docs/EXTENSIONS.md.

## Turn one on

Put the extension's folder in `custom/game-extensions/<name>/` and list it in `config/zellera.local.env`:

```
ZEL_GAME_EXTENSIONS=<name>
```

The list takes names (folders under `custom/game-extensions`) and absolute folders, comma separated. The server reads
it when it starts, never from the settings table. An extension that cannot load is logged and left out; the game
starts without it.

## The folder

```
custom/game-extensions/<name>/
  extension.json
  <name>.jar
  ...            any data files the extension reads from its folder
```

`extension.json`:

```json
{
  "name": "racing",
  "version": "1.0.0",
  "description": "Kart races with lap times.",
  "jar": "racing.jar",
  "main": "org.example.racing.Racing",
  "server": ">=1.0.0 <2.0.0",
  "settings": { "laps": "3" }
}
```

| Field | What |
|---|---|
| `name` | a-z and 0-9, 2 to 32 characters, the same as the folder |
| `version` | the extension's own version, like `1.0.0` |
| `jar` | the jar in the folder |
| `main` | the class that implements `zel.ext.GameExtension`, with a constructor without arguments |
| `server` | optional: the server versions it runs on (`>=`, `<=`, `>`, `<`, `=`; all parts must hold) |
| `settings` | optional: default values the extension reads with `host.setting(key)` |

A setting's default can be changed with a row `extensions.<name>.<key>` in the `settings` table.

## Writing one

Build against the server's jar (the bundle's `zellera-server-<version>-all.jar`, or
`server/build/libs/zellera-server-<version>-all.jar` after `./gradlew shadowJar` in a source checkout) as a
compile-only dependency; the server brings Kotlin and its libraries at run time.

```kotlin
class Racing : GameExtension {
    override fun start(host: ExtensionHost) {
        host.onEvent { scope, event ->
            if (event is RoomEvent.Entered) {
                val unit = scope.room.units[event.unitIndex] ?: return@onEvent
                host.rooms.say(scope, unit, "Ready to race!")
            }
        }
        host.onTick { scope -> /* every 500 ms while players are in the room */ }
    }
}
```

`start` runs once, before the first room loads. Register the hooks there:

| Hook | When |
|---|---|
| `onEvent` | every room event: enter, leave, say, step on, click furni, click user, state change, ... |
| `onTick` | every room tick (500 ms) while players are in the room |
| `onRoom` | a room loads or unloads |
| `onClick` | a walk click; answer true to take it (no walk starts) |
| `onArrival` | a player enters a room; return a spot to place the avatar there |
| `onPacketIn(codec)` | every message of that kind a client sends, before the server handles it |
| `onPacketOut(codec)` | every message of that kind the server sends, once per receiving player |
| `onServerEvent` | logins, logouts, room entries and exits, catalogue purchases |

The room hooks (`onEvent`, `onTick`, `onRoom`, `onClick`, `onArrival`) run on the room's own thread under its lock,
like the room's built-in parts. They must be quick: never wait on the database or the network there. A hook that
throws is logged and skipped; the room goes on.

### Packets and server events

`onPacketIn` and `onPacketOut` see the typed message (the codecs in `zel.protocol.generated`) and the player it is
from or for (null before login). A hook returns the message to pass it on, another message to replace it, or null to
take it: an incoming message the server then does not handle, an outgoing one that player does not get. A message the
server has no handler for reaches `onPacketIn` too, so an extension can answer messages of its own. `host.send`
sends a message to one online player.

```kotlin
// "/help" in the chat never reaches the room; the player gets a notification instead
host.onPacketIn(ChatInCodec) { playerId, chat ->
    if (playerId == null || chat.string1 != "/help") return@onPacketIn chat
    host.send(playerId, NotificationDialogOutCodec, NotificationDialogOut("help", listOf()))
    null
}
host.onServerEvent { event -> if (event is ServerEvent.LoggedIn) host.values.prefetch(event.playerId) }
```

These hooks run on the thread that handles the packet or event (a connection's lane, a room's lane, a game thread),
not under a room's lock: keep them quick and thread-safe, and change a room through `host.rooms.post`. With no
extension hooking a message, it costs nothing.

### Room actions (`host.rooms`)

| Action | What |
|---|---|
| `addFurni` / `removeFurni` | furni that lives only while the room is loaded: never saved, picked up or traded |
| `setState` | change a furni's state and show it |
| `moves(scope).furni(...).unit(...).send()` | furni and avatars slide together; a seated avatar stays seated |
| `place`, `sit`, `lie`, `stand` | put an avatar somewhere, give it a posture |
| `sit` | a seat that holds where no seat furni stands (a vehicle): the avatar stays seated until `stand` or `place` |
| `addBot` / `removeBot` | a bot that lives only while the room is loaded (passers-by, drivers): never saved or picked up |
| `walkTo` | walk an avatar or bot to a tile |
| `closeTiles` | close floor tiles to walks (and open them again) |
| `userClicks` | have clients report clicks on users (the `ClickUser` event) without a wired box in the room (set it in `onRoom` load) |
| `viewDistance` | large rooms: each player gets only the floor furni near their avatar (set it in `onRoom` load); a player can get a distance of their own |
| `floor` | the room's floor comes from the extension when the room loads: any size, heights in fine steps. A floor larger than a client's floor map can carry (about 255 x 255 tiles) reaches clients as an empty map: walking and what there is to see are then the extension's own work |
| `showScenery`, `hideScenery` | floor pictures for one player only: no furni of the room, nothing to walk on or use. For a world too large to send whole |
| `say`, `whisper`, `notify` | talk as a unit, whisper to one, send a client notification |
| `travel` | send a player to another room; `onArrival` decides where they appear |
| `post` | run something on a room's thread later, or from outside its hooks |
| `nameOf`, `definition`, `roomOf` | look things up |

### Player numbers (`host.values`)

Whole numbers per player and name, stored in `extension_player_values` under the extension's name. A player's
numbers load in the background the first time they are read (`get` returns null until then, `prefetch` starts it
early); writes change memory at once and are saved in the background, in order.
