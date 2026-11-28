package zel.core.config

import io.netty.channel.WriteBufferWaterMark
import zel.GameBootstrap
import zel.core.CommandLog
import zel.core.config.ConfigKey.Kind
import zel.core.load.LoadWatch
import zel.db.Database
import zel.db.WriterSize
import zel.game.camera.CameraAssets
import zel.game.camera.CameraService
import zel.game.players.TICKET_ADDRESS_KEY
import zel.net.NetModule
import zel.net.TransportSettings
import zel.net.WireLimits
import zel.net.clients.FailedTickets
import zel.net.server.WorkerSettings
import zel.protocol.handshake.LoginAdmission
import zel.protocol.handshake.LoginCrowd
import zel.protocol.handshake.PacketShuffling
import zel.room.RoomCheckpoints
import zel.room.rights.RaidWatch
import zel.security.ClientCheck
import zel.security.Suspicions
import zel.wired.effect.user.CopyAllowance

internal object ConfigKeys {
    private const val DEFAULT_GAME_PORT = 2096
    private const val KIB = 1024
    private const val MIB = KIB * KIB

    fun all(): List<ConfigKey> =
        buildList {
            client()
            database()
            encryption()
            network()
            http()
            runtime()
            raids()
            botGuard()
            resilience()
            operation()
            zone()
            game()
        }

    private class Area(
        private val prefix: String,
        private val into: MutableList<ConfigKey>,
    ) {
        fun string(
            name: String,
            description: String,
            default: String = "",
        ) = add(name, Kind.STRING, default, description)

        fun int(
            name: String,
            default: Int,
            description: String,
        ) = add(name, Kind.INTEGER, default.toString(), description)

        fun long(
            name: String,
            default: Long,
            description: String,
        ) = add(name, Kind.LONG, default.toString(), description)

        fun flag(
            name: String,
            default: Boolean,
            description: String,
        ) = add(name, Kind.BOOLEAN, default.toString(), description)

        private fun add(
            name: String,
            type: Kind,
            default: String,
            description: String,
        ) {
            into += ConfigKey("$prefix.$name", type, default, description)
        }
    }

    private fun MutableList<ConfigKey>.area(
        prefix: String,
        declare: Area.() -> Unit,
    ) = Area(prefix, this).declare()

    private fun MutableList<ConfigKey>.client() {
        area("login") {
            int(
                LoginCrowd.DEVICE_KEY.removePrefix("login."),
                LoginCrowd.PER_DEVICE,
                "Players one device may have online at once; 0 is no cap.",
            )
            int(
                LoginCrowd.ADDRESS_KEY.removePrefix("login."),
                LoginCrowd.PER_ADDRESS,
                "Players one client address may have online at once; 0 is no cap. Loopback addresses are not counted.",
            )
            string(
                "client_releases",
                "Client version numbers a login may come from, comma separated; `any` lets in every version the " +
                    "game has ids for.",
            )
            flag(
                "device.is_checked",
                default = true,
                description =
                    "Refuse a login whose client is not on the device the play page measured for its ticket; " +
                        "from ZEL_DEVICE_CHECK.",
            )
            int(
                "device.ban.days",
                default = 30,
                description =
                    "Days after its last login a device of a banned player still refuses the logins of other " +
                        "players from it.",
            )
            int(
                "device.ban.others",
                default = 0,
                description =
                    "Players beside the banned one and the one logging in who may share graphics card, screen " +
                        "and fonts before the three stop counting as the banned player's device.",
            )
        }
        area("server") {
            string("console.colour", "Console colour: `on`, `off`, or `auto`, which colours only a terminal.", "auto")
        }
    }

    private fun MutableList<ConfigKey>.database() =
        area("database") {
            string("host", "Database server host; from ZEL_DB_HOST.", Database.DEFAULT_HOST)
            int("port", Database.DEFAULT_PORT, "Database server port; from ZEL_DB_PORT.")
            string("name", "Database name; from ZEL_DB_NAME.")
            string(
                "user",
                "Database account: ZEL_DB_APP_USER, or the schema owner ZEL_DB_MIGRATE_USER for a schema step or " +
                    "a content import.",
            )
            string("password", "Password of that account: ZEL_DB_APP_PASS or ZEL_DB_MIGRATE_PASS. Never logged.")
            string("url_parameters", "Extra JDBC URL parameters.")
            int("pool.max_connections", Database.DEFAULT_CONNECTIONS, "Most open connections.")
            int(
                "pool.idle_connections",
                Database.DEFAULT_SPARE_CONNECTIONS,
                "Idle connections kept open.",
            )
            long("pool.wait.ms", 0, "Longest wait for a free connection; 0 keeps the pool's own.")
            long("pool.idle.ms", 0, "Idle time after which a spare connection closes; 0 keeps the pool's own.")
            long("pool.lifetime.ms", 0, "Age at which a connection is replaced; 0 keeps the pool's own.")
            long("pool.check.ms", 0, "Longest a connection check may take; 0 keeps the pool's own.")
            long("pool.leak_report.ms", 0, "Report a connection held longer than this; 0 is off.")
            int(
                Database.SILENT_KEY.removePrefix("database."),
                Database.SILENT_CONNECTION.seconds.toInt(),
                "Seconds a connection may answer nothing before it is given up.",
            )
        }

    private fun MutableList<ConfigKey>.encryption() {
        area("login") {
            flag(
                "key_exchange.is_on",
                false,
                "Key exchange with the client and a scrambled stream after it; on with the key in the secrets " +
                    "folder. It only keeps the traffic from being read off the browser's own tools: it is not " +
                    "what protects a connection, TLS on the site's address is.",
            )
            string("key_exchange.modulus", "Modulus of the RSA key, hex; from the key file.")
            string(
                "key_exchange.private_exponent",
                "Private exponent of the RSA key, hex; from the key file. Never logged.",
            )
            flag(
                "key_exchange.is_required",
                false,
                "Refuse a login that skipped the key exchange while a key is set; off by default. It adds no " +
                    "security of its own.",
            )
            int(
                FailedTickets.LIMIT_KEY.removePrefix("login."),
                FailedTickets.DEFAULT_LIMIT,
                "Failed login tickets one client address may have counting against it before its logins are " +
                    "turned away unread; 0 counts none. An IPv6 address counts as its /64, loopback is not counted.",
            )
            int(
                FailedTickets.WINDOW_KEY.removePrefix("login."),
                FailedTickets.DEFAULT_WINDOW_SECONDS,
                "Seconds over which that many failures are forgiven again, one at a time.",
            )
        }
    }

    private fun MutableList<ConfigKey>.network() {
        area("network") {
            string("listen.address", "Address the game listener binds to.", "127.0.0.1")
            int("listen.port", DEFAULT_GAME_PORT, "Port of the game listener.")
            string(
                "origin.hosts",
                "Host names the WebSocket handshake accepts, comma separated; `*` is any, empty is none. " +
                    "Set from the site's address and its extra origins.",
                "",
            )
            flag(
                "client_address.trusts_cloudflare",
                false,
                "Also trust the client address header from Cloudflare's published edge addresses.",
            )
            int("connections.max", NetModule.DEFAULT_MAX_CONNECTIONS, "Most game sockets open at a time.")
            int(
                "connections.per_address",
                NetModule.DEFAULT_PER_ADDRESS,
                "Most game sockets one client address holds; 0 is no cap. Loopback addresses are not counted. " +
                    "From ZEL_MAX_CONNECTIONS_PER_IP.",
            )
        }
        networkWork()
        networkLimits()
    }

    private fun MutableList<ConfigKey>.networkWork() {
        area("network") {
            int("accept.threads", 0, "Threads accepting game connections; 0 takes twice the CPUs.")
            int("io.threads", 0, "Threads reading and writing game connections; 0 takes twice the CPUs.")
            int("packets.threads", 0, "Packet handler threads; 0 takes twice the CPUs, 16 at least.")
            int(
                "packets.backlog.max",
                0,
                "Packets all connections together may have waiting or running; 0 takes " +
                    "${WorkerSettings.PACKETS_PER_THREAD} for every packet thread.",
            )
            int(
                "packets.backlog.per_connection",
                0,
                "Packets one connection may have waiting or running; 0 takes what it may send in " +
                    "${WorkerSettings.SECONDS_PER_CONNECTION} seconds (limits.messages_per_second of the game rules).",
            )
            int(
                "packets.backlog.max.bytes",
                0,
                "Bytes of packet bodies all connections together may have waiting or running; 0 takes " +
                    "${WorkerSettings.BYTES_PER_THREAD / MIB} MiB for every packet thread. Over it, a connection " +
                    "holding more than ${WireLimits.DEFAULT_ORDINARY_FRAME_BYTES / KIB} KiB stops reading; one " +
                    "sending small packets reads on.",
            )
            int(
                "packets.backlog.per_connection.bytes",
                0,
                "Bytes of packet bodies one connection may have waiting or running; 0 takes " +
                    "${WorkerSettings.LARGEST_FRAMES_PER_CONNECTION} frames of limits.frame_bytes of the game rules, " +
                    "never above packets.backlog.max.bytes.",
            )
            flag(
                "packets.backlog.pauses_reading",
                true,
                "At any backlog limit the server stops reading from the connection until its packets are " +
                    "done; off, the backlogs are only counted.",
            )
            int(
                "output.resume.bytes",
                WriteBufferWaterMark.DEFAULT.low(),
                "Bytes of unsent output under which a connection is no longer watched.",
            )
            int(
                "output.pause.bytes",
                WriteBufferWaterMark.DEFAULT.high(),
                "Bytes of unsent output above which a connection is watched for a stall.",
            )
            int(
                "output.stalled.seconds",
                TransportSettings.DEFAULT_STALLED_SECONDS,
                "Seconds a watched connection may take no output at all before it closes; a slow one stays.",
            )
            int(
                "output.backlog.max.bytes",
                TransportSettings.DEFAULT_BACKLOG_BYTES,
                "Bytes of unsent output at which a connection closes however fast it reads.",
            )
        }
    }

    private fun MutableList<ConfigKey>.networkLimits() =
        area("network") {
            int(
                "ping.gap.seconds",
                WireLimits.DEFAULT_PING_GAP_SECONDS,
                "Seconds a connection may be silent before the server pings it.",
            )
            int(
                "ping.timeout.seconds",
                WireLimits.DEFAULT_PING_TIMEOUT_SECONDS,
                "Seconds a pinged connection has to send anything before it closes.",
            )
            int(
                "login.timeout.seconds",
                WireLimits.DEFAULT_LOGIN_SECONDS,
                "Seconds a new connection has to log in before it closes.",
            )
            int(
                "upgrade.timeout.seconds",
                WireLimits.DEFAULT_UPGRADE_TIMEOUT_SECONDS,
                "Seconds a proxied connection has to upgrade to WebSocket before it closes.",
            )
            int(
                "connections.pending_per_proxy",
                WireLimits.DEFAULT_PENDING_PER_PROXY,
                "Most un-upgraded sockets one trusted proxy address holds at a time; 0 is no cap.",
            )
            int(
                "upgrade.max.bytes",
                WireLimits.DEFAULT_UPGRADE_BYTES,
                "Largest request that opens a game connection.",
            )
            int(
                "bridge.request.max.bytes",
                WireLimits.DEFAULT_BRIDGE_BYTES,
                "Largest bridge request the site may send.",
            )
        }

    private fun MutableList<ConfigKey>.http() {
        area("network") {
            int(
                "bridge.threads",
                WorkerSettings.BRIDGE_THREADS,
                "Threads for the site's bridge requests on the game port.",
            )
            int(
                "bridge.backlog.max",
                0,
                "Bridge requests that may run or wait at once, more are answered 503; 0 takes " +
                    "${WorkerSettings.REQUESTS_PER_THREAD} for every bridge thread.",
            )
        }
        area("wired") {
            string(
                "variables.bridge.secret",
                "Shared secret of the web server's Wired Variables bridge (ZEL_SECRETS_DIR/wired-bridge-secret, " +
                    "64 hex); empty turns the bridge off. Never logged.",
            )
        }
    }

    private fun MutableList<ConfigKey>.resilience() {
        area("server") {
            flag(
                "load.refuses_logins",
                false,
                "Turn new logins away while the load is high; off, a high load is only logged.",
            )
            int(
                "load.refuse.percent",
                LoadWatch.Settings.REFUSE_PERCENT,
                "Smoothed use of the fullest part (database connections, write queue, packets, scheduler " +
                    "lateness, memory left after collection) from which the load counts as high.",
            )
            int(
                "load.resume.percent",
                LoadWatch.Settings.RESUME_PERCENT,
                "Smoothed use every part must be under before the load counts as normal again.",
            )
            int(
                "load.smoothing.seconds",
                LoadWatch.Settings.SMOOTHING_SECONDS,
                "Time over which the use is smoothed: shorter peaks pass, a lasting load shows after about twice " +
                    "this.",
            )
            int(
                "load.scheduler.late.ms",
                LoadWatch.Settings.SCHEDULER_LATE_MILLIS,
                "Wait of the scheduler's most overdue task that counts as a full scheduler.",
            )
        }
    }

    private fun MutableList<ConfigKey>.operation() {
        area("server") {
            int(
                CommandLog.LIMIT_KEY.removePrefix("server."),
                CommandLog.DEFAULT_LIMIT,
                "Logged command runs that may wait for the database; past it the oldest are let go.",
            )
            flag("log.is_debug", false, "Log at debug level (very verbose; never on a busy game).")
            flag("console.reads_commands", false, "Read operator commands from the server console.")
        }
    }

    /** The game's time zone, a startup key from ZEL_TIME_ZONE over its settings row. */
    private fun MutableList<ConfigKey>.zone() {
        area("game") {
            string(
                "time_zone",
                "Time zone of the dates and times players see and of the game's local days, as a name such as " +
                    "Europe/Amsterdam; empty takes the settings row, then the machine's. From ZEL_TIME_ZONE.",
            )
        }
    }

    private fun MutableList<ConfigKey>.camera() =
        area("camera") {
            int(
                CameraAssets.KEY.removePrefix("camera."),
                default = CameraAssets.DEFAULT_MAX_BYTES,
                description = "Bytes of furni and avatar art the photo renderer may keep in memory.",
            )
            int(
                "competition.entries_per_day",
                default = 3,
                description = "Photo competition entries one player may send a day; at least 1.",
            )
            int(
                CameraService.PHOTOS_PER_DAY_KEY.removePrefix("camera."),
                default = CameraService.DEFAULT_PHOTOS_PER_DAY,
                description = "Different photos one player may buy or publish a day; at least 1.",
            )
        }

    private fun MutableList<ConfigKey>.game() {
        camera()
        area("respect") {
            int("buybacks.per_day", default = 1, description = "Times a day a player may buy their respects back.")
        }
        area("daily_tasks") {
            int("per_day", default = 3, description = "Daily tasks a player is given each day.")
        }
        area("featured_looks") {
            int(
                "cache.seconds",
                default = 300,
                description = "Seconds the featured looks are kept before they are read again.",
            )
        }
        area("player") {
            int("motto.max.characters", default = 38, description = "Longest motto a player may set.")
            int("name_change.reserve_days", 0, "Days a freed name stays kept for its old owner; 0 keeps none.")
        }
        area("mystery_box") {
            string("tracker.box_colour", "Colour of the mystery box the tracker shows; empty shows none.")
            string("tracker.key_colour", "Colour of the mystery key the tracker shows; empty shows none.")
        }
        area("pets") {
            int(
                "respect.account_age.days",
                default = 0,
                description = "Days an account must exist before it may respect a pet; 0 is no wait.",
            )
        }
        area("recycler") {
            int("furni_per_prize", default = 8, description = "Furni the recycler takes for one prize, 1 to 12.")
        }
        area("furni") {
            int("rentable_space.rent.days", default = 7, description = "Days one rent of a rentable space lasts.")
        }
        area("moderation") {
            string("caution.link", "Link a caution sends along; empty sends a plain ban notice instead.")
        }
        area("wired") {
            int(
                "monitor.usage_allowance",
                default = 0,
                description =
                    "Wired usage allowance of every room, whatever its owner's level; 0 follows the owner's " +
                        "account level.",
            )
            int(
                CopyAllowance.MAX_LIVE_KEY.removePrefix("wired."),
                default = CopyAllowance.MAX_LIVE,
                description = "Place furni copies a room may hold at once; a reload of the room removes them.",
            )
        }
        area("furni") {
            string(
                "youtube.api_key",
                "YouTube Data API key the TV playlists are listed with; empty leaves the TVs empty.",
            )
        }
    }

    private fun MutableList<ConfigKey>.botGuard() {
        area("login") {
            flag(
                PacketShuffling.ON_KEY.removePrefix("login."),
                false,
                "Shuffle the packet ids of every session after the key exchange, from a key both sides work out " +
                    "on their own; needs the key exchange and the client's shuffle extension, and the site turns " +
                    "the client's half on from the same setting.",
            )
            int(
                PacketShuffling.STRIKES_KEY.removePrefix("login."),
                PacketShuffling.STRIKES,
                "Packet ids a shuffled session may send that it does not know before it is closed; 0 never closes.",
            )
            flag(
                TICKET_ADDRESS_KEY.removePrefix("login."),
                true,
                "Refuse a login ticket used from another address than the one the site made it for; an IPv6 " +
                    "address counts as its /64, and an IPv4 and an IPv6 address are never compared.",
            )
            string(
                LoginAdmission.BUILD.removePrefix("login."),
                "The exact client release this hotel serves, as in client.json; a hello naming another is " +
                    "refused. Empty allows any.",
            )
        }
        area("security") {
            int(
                Suspicions.MUTE_SCORE_KEY.removePrefix("security."),
                Suspicions.MUTE_SCORE,
                "Suspicion score at which a player's chat is only shown to the player; 0 never mutes.",
            )
            int(
                Suspicions.WINDOW_KEY.removePrefix("security."),
                Suspicions.WINDOW_HOURS,
                "Hours of suspicious activity that count toward that score.",
            )
            flag(
                ClientCheck.ON_KEY.removePrefix("security."),
                false,
                "Once a session, 30 to 300 seconds after login, ask the client for a keyed hash of a random part of " +
                    "the launcher.js the hotel serves; a wrong answer or none in 20 seconds is a suspicion. Needs a " +
                    "client release that answers.",
            )
            string(
                ClientCheck.FOLDER_KEY.removePrefix("security."),
                "The client folder the web serves, whose client.json names the launcher the check reads; from " +
                    "ZEL_CLIENT_DIR.",
            )
        }
    }

    private fun MutableList<ConfigKey>.raids() {
        area("room") {
            int(
                RaidWatch.WINDOW_KEY.removePrefix("room."),
                RaidWatch.WINDOW_SECONDS,
                "Seconds raid protection looks back over for suspicious newcomers.",
            )
            int(
                RaidWatch.SUSPECTS_LOW_KEY.removePrefix("room."),
                RaidWatch.SUSPECTS_LOW,
                "Suspicious newcomers within the window that make a raid at low sensitivity.",
            )
            int(
                RaidWatch.SUSPECTS_MEDIUM_KEY.removePrefix("room."),
                RaidWatch.SUSPECTS_MEDIUM,
                "Suspicious newcomers within the window that make a raid at medium sensitivity.",
            )
            int(
                RaidWatch.SUSPECTS_HIGH_KEY.removePrefix("room."),
                RaidWatch.SUSPECTS_HIGH,
                "Suspicious newcomers within the window that make a raid at high sensitivity.",
            )
            int(
                RaidWatch.NEW_ACCOUNT_KEY.removePrefix("room."),
                RaidWatch.NEW_ACCOUNT_HOURS,
                "Hours an account counts as new for raid protection.",
            )
            int(
                RaidWatch.SAME_LINE_KEY.removePrefix("room."),
                RaidWatch.SAME_LINE_PLAYERS,
                "Newcomers saying the same line within the window that makes each of them suspicious.",
            )
        }
    }

    private fun MutableList<ConfigKey>.runtime() {
        area("server") {
            int(
                "scheduler.threads",
                0,
                "Threads of the shared scheduler; 0 takes one per processor, four at least.",
            )
        }
        area("database") {
            int(
                "writer.threads",
                0,
                "Database writer threads, at most the pool's connections; 0 takes a third of the pool, " +
                    "${WriterSize.LEAST_THREADS} at least.",
            )
            int(
                WriterSize.QUEUE_KEY.removePrefix("database."),
                WriterSize.QUEUE_PER_THREAD,
                "Writes that may wait for each writer thread; with the queue full the caller waits.",
            )
        }
        area("room") {
            int(
                "checkpoint.interval.minutes",
                RoomCheckpoints.DEFAULT_INTERVAL_MINUTES,
                "Minutes between two saves of a loaded room's furni moves and states and its permanent wired " +
                    "variables; 0 saves them only when the room unloads and at a clean stop. " +
                    "From ZEL_ROOM_SAVE_MINUTES.",
            )
        }
        area("server") {
            int(
                "stop.write_drain.seconds",
                GameBootstrap.DEFAULT_DRAIN_SECONDS,
                "Longest the stop waits for queued database writes, at most ${GameBootstrap.MAX_DRAIN_SECONDS}: " +
                    "the rest of the time the operator command gives a stop is for the rooms.",
            )
        }
    }
}
