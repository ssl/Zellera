-- The baseline of a new game: every table, then the rows every game starts with before any content pack.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET foreign_key_checks = 0;


-- Server: settings, texts and the bookkeeping of the server itself.

CREATE TABLE `settings` (
  `name` varchar(100) NOT NULL,
  `value` varchar(512) NOT NULL,
  `description` text NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Server settings.';

CREATE TABLE `texts` (
  `name` varchar(100) NOT NULL,
  `value` varchar(4096) NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Server-side texts the game sends in its own words.';

CREATE TABLE `client_strings` (
  `name` varchar(100) NOT NULL,
  `value` varchar(512) NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Values the client needs byte for byte; a pack''s client profile brings them.';

CREATE TABLE `content_packs` (
  `name` varchar(100) NOT NULL,
  `version` varchar(64) NOT NULL,
  `format` int(11) NOT NULL DEFAULT 0,
  `sha256` char(64) NOT NULL,
  `asset_location` varchar(512) NOT NULL DEFAULT '',
  `imported_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The imported content pack.';

CREATE TABLE `daily_chores` (
  `chore` varchar(32) NOT NULL,
  `done_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`chore`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The moment each recurring chore was last done.';

CREATE TABLE `web_commands` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) NOT NULL,
  `kind` varchar(32) NOT NULL,
  `payload` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`payload`)),
  `created_by` int(11) DEFAULT NULL,
  `created_at` int(10) unsigned NOT NULL,
  `status` enum('pending','done','failed') NOT NULL DEFAULT 'pending',
  `result` varchar(255) DEFAULT NULL,
  `processed_at` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_web_commands_1` (`status`,`id`),
  KEY `ix_web_commands_2` (`player_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Queue from the web to the game server.';


-- Ranks and permissions

CREATE TABLE `ranks` (
  `id` int(11) NOT NULL,
  `name` varchar(25) NOT NULL,
  `badge_code` varchar(12) NOT NULL DEFAULT '',
  `log_commands` tinyint(1) NOT NULL DEFAULT 0,
  `max_rooms` int(11) NOT NULL DEFAULT 0,
  `max_friends` int(11) NOT NULL DEFAULT 0,
  `max_favourite_rooms` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Staff and player ranks.';

CREATE TABLE `permissions` (
  `code` varchar(64) NOT NULL,
  `description` text NOT NULL,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Every permission the code asks for.';

CREATE TABLE `rank_permissions` (
  `rank_id` int(11) NOT NULL,
  `permission_code` varchar(64) NOT NULL,
  PRIMARY KEY (`rank_id`,`permission_code`),
  KEY `fk_rank_permissions_2` (`permission_code`),
  CONSTRAINT `fk_rank_permissions_1` FOREIGN KEY (`rank_id`) REFERENCES `ranks` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_rank_permissions_2` FOREIGN KEY (`permission_code`) REFERENCES `permissions` (`code`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The permissions a rank holds: a row is the grant, no row is no grant.';

CREATE TABLE `rank_changes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) NOT NULL,
  `old_rank` int(11) NOT NULL,
  `new_rank` int(11) NOT NULL,
  `changed_at` int(10) unsigned NOT NULL,
  `actor_id` int(11) DEFAULT NULL COMMENT 'NULL: the game itself, such as a temporary rank running out.',
  `via` varchar(64) NOT NULL DEFAULT '' COMMENT 'Where the change came from, such as housekeeping.',
  PRIMARY KEY (`id`),
  KEY `ix_rank_changes_1` (`player_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Every change of a player''s rank and who made it, shown in staff pages.';

CREATE TABLE `effect_rank_limits` (
  `effect_id` int(11) NOT NULL,
  `min_rank` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`effect_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Avatar effects only some ranks may enable.';


-- Players

CREATE TABLE `players` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `account_id` int(11) DEFAULT NULL COMMENT 'The account (player_accounts) this avatar belongs to; NULL for the content accounts, which never log in.',
  `name` varchar(50) NOT NULL,
  `motto` varchar(127) NOT NULL DEFAULT '',
  `figure` varchar(256) NOT NULL,
  `gender` enum('M','F') NOT NULL DEFAULT 'M',
  `rank_id` int(11) NOT NULL,
  `is_online` tinyint(1) NOT NULL DEFAULT 0,
  `home_room_id` int(11) DEFAULT NULL,
  `created_at` int(10) unsigned NOT NULL,
  `last_login_at` int(10) unsigned DEFAULT NULL,
  `last_online_at` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_players_1` (`name`),
  KEY `ix_players_1` (`last_online_at`),
  KEY `fk_players_1` (`rank_id`),
  KEY `fk_players_2` (`home_room_id`),
  KEY `ix_players_2` (`account_id`),
  CONSTRAINT `fk_players_1` FOREIGN KEY (`rank_id`) REFERENCES `ranks` (`id`) ON DELETE NO ACTION,
  CONSTRAINT `fk_players_2` FOREIGN KEY (`home_room_id`) REFERENCES `rooms` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_players_3` FOREIGN KEY (`account_id`) REFERENCES `player_accounts` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The player as the game and other players see them. Ids start at 1; no row stands for the server or for nobody.';

CREATE TABLE `player_accounts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) DEFAULT NULL COMMENT 'The avatar (players) the login ticket is for: the one the account played as last.',
  `password_hash` varchar(64) NOT NULL,
  `email` varchar(254) DEFAULT NULL COMMENT 'The login e-mail address, unique; NULL while the site does not ask for one (ZEL_WEB_EMAIL).',
  `email_verified` tinyint(1) NOT NULL DEFAULT 0,
  `registered_ip` varchar(45) NOT NULL,
  `last_ip` varchar(45) NOT NULL,
  `machine_id` varchar(64) NOT NULL DEFAULT '',
  `login_ticket` varchar(256) DEFAULT NULL,
  `login_ticket_expires_at` int(10) unsigned DEFAULT NULL,
  `login_ticket_device` varchar(120) DEFAULT NULL COMMENT 'The device reading the play page stored with the login ticket; NULL until it is measured and after the ticket is used.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_player_accounts_1` (`login_ticket`),
  UNIQUE KEY `uq_player_accounts_2` (`player_id`),
  UNIQUE KEY `uq_player_accounts_3` (`email`),
  KEY `ix_player_accounts_1` (`registered_ip`),
  KEY `ix_player_accounts_2` (`last_ip`),
  KEY `ix_player_accounts_3` (`machine_id`),
  CONSTRAINT `fk_player_accounts_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='An account: the web login (e-mail, password) and network facts its avatars (players.account_id) share. Only login, registration and staff pages read it.';

CREATE TABLE `player_devices` (
  `player_id` int(11) NOT NULL,
  `id` char(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'The random id the browser keeps.',
  `gpu` char(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `scr` char(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `cpu` char(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `tz` char(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `fnt` char(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `cnv` char(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `aud` char(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `gl` char(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `ua` char(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `first_seen_at` int(10) unsigned NOT NULL,
  `last_seen_at` int(10) unsigned NOT NULL,
  `logins` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`player_id`,`id`,`gpu`,`scr`,`cpu`,`tz`,`fnt`,`cnv`,`aud`,`gl`,`ua`),
  KEY `ix_player_devices_1` (`id`),
  KEY `ix_player_devices_2` (`gpu`,`scr`,`fnt`),
  CONSTRAINT `fk_player_devices_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The devices a player logged in from, one row per distinct reading. A piece of all zeros is one the browser does not give, one of all f''s cannot be trusted.';

CREATE TABLE `player_standing` (
  `player_id` int(11) NOT NULL,
  `safety_locked` tinyint(1) NOT NULL DEFAULT 0,
  `can_trade` tinyint(1) NOT NULL DEFAULT 1,
  `trade_locked_until` int(10) unsigned DEFAULT NULL,
  `trade_lock_count` int(11) NOT NULL DEFAULT 0,
  `muted_until` int(10) unsigned DEFAULT NULL,
  `may_change_name` tinyint(1) NOT NULL DEFAULT 0,
  `welcome_gift_taken` tinyint(1) NOT NULL DEFAULT 0,
  `reports_sent` int(11) NOT NULL DEFAULT 0,
  `reports_abusive` int(11) NOT NULL DEFAULT 0,
  `warnings_received` int(11) NOT NULL DEFAULT 0,
  `bans_received` int(11) NOT NULL DEFAULT 0,
  `max_rooms` int(11) NOT NULL DEFAULT 50,
  `max_friends` int(11) NOT NULL DEFAULT 500,
  PRIMARY KEY (`player_id`),
  CONSTRAINT `fk_player_standing_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What a player may do: locks, staff counters, limits.';

CREATE TABLE `player_stats` (
  `player_id` int(11) NOT NULL,
  `achievement_score` int(11) NOT NULL DEFAULT 0,
  `daily_respect_left` int(11) NOT NULL DEFAULT 3,
  `daily_pet_respect_left` int(11) NOT NULL DEFAULT 3,
  `daily_respect_refills_left` int(11) NOT NULL DEFAULT 0,
  `respect_given` int(11) NOT NULL DEFAULT 0,
  `respect_received` int(11) NOT NULL DEFAULT 0,
  `online_seconds` int(11) NOT NULL DEFAULT 0,
  `login_streak` int(11) NOT NULL DEFAULT 0,
  `hall_of_fame_points` int(11) NOT NULL DEFAULT 0,
  `forum_post_count` int(11) NOT NULL DEFAULT 0,
  `favourite_guild_id` int(11) DEFAULT NULL,
  `club_gifts_claimed` int(11) NOT NULL DEFAULT 0,
  `last_club_payday_at` int(10) unsigned DEFAULT NULL,
  `build_pass_bonus_furni` int(11) NOT NULL DEFAULT 0,
  `rented_space_furni_id` int(11) DEFAULT NULL,
  `rented_space_ends_at` int(10) unsigned DEFAULT NULL,
  `marketplace_tokens` int(11) NOT NULL DEFAULT 0,
  `new_additions_seen` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`player_id`),
  KEY `ix_player_stats_1` (`achievement_score`),
  KEY `ix_player_stats_2` (`online_seconds`),
  KEY `ix_player_stats_3` (`hall_of_fame_points`),
  KEY `fk_player_stats_2` (`favourite_guild_id`),
  CONSTRAINT `fk_player_stats_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_player_stats_2` FOREIGN KEY (`favourite_guild_id`) REFERENCES `guilds` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Counters and running state of a player, written by the save on leaving.';

CREATE TABLE `player_account_levels` (
  `player_id` int(11) NOT NULL,
  `achievement_points` int(11) NOT NULL DEFAULT 0,
  `badge_points` int(11) NOT NULL DEFAULT 0,
  `hosting_level_minutes` bigint(20) NOT NULL DEFAULT 0,
  `furni_points` int(11) NOT NULL DEFAULT 0,
  `counted_points` int(11) NOT NULL DEFAULT 0,
  `level` tinyint(4) NOT NULL DEFAULT 1,
  `rewarded_level` tinyint(4) NOT NULL DEFAULT 1,
  PRIMARY KEY (`player_id`),
  CONSTRAINT `fk_player_account_levels_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The account level of a player and the scores behind it.';

CREATE TABLE `player_preferences` (
  `player_id` int(11) NOT NULL,
  `block_following` tinyint(1) NOT NULL DEFAULT 0,
  `block_friend_requests` tinyint(1) NOT NULL DEFAULT 0,
  `block_room_invites` tinyint(1) NOT NULL DEFAULT 0,
  `block_camera_follow` tinyint(1) NOT NULL DEFAULT 0,
  `hide_online` tinyint(1) NOT NULL DEFAULT 0,
  `hide_profile` tinyint(1) NOT NULL DEFAULT 0,
  `ignore_bots` tinyint(1) NOT NULL DEFAULT 0,
  `ignore_pets` tinyint(1) NOT NULL DEFAULT 0,
  `volume_system` tinyint(4) NOT NULL DEFAULT 100,
  `volume_furni` tinyint(4) NOT NULL DEFAULT 100,
  `volume_music` tinyint(4) NOT NULL DEFAULT 100,
  `old_chat` tinyint(1) NOT NULL DEFAULT 0,
  `chat_color` int(11) NOT NULL DEFAULT 0,
  `chat_mode` tinyint(4) NOT NULL DEFAULT 0,
  `chat_bubble_width` tinyint(4) NOT NULL DEFAULT 1,
  `chat_scroll_speed` tinyint(4) NOT NULL DEFAULT 1,
  `online_indicator` tinyint(4) NOT NULL DEFAULT 0,
  `wired_whisper_off` tinyint(1) NOT NULL DEFAULT 0,
  `wired_menu_button` tinyint(1) NOT NULL DEFAULT 0,
  `wired_inspect_button` tinyint(1) NOT NULL DEFAULT 0,
  `wired_playtest` tinyint(1) NOT NULL DEFAULT 0,
  `ui_flags` int(11) NOT NULL DEFAULT 1,
  `saved_searches_seeded` tinyint(1) NOT NULL DEFAULT 0,
  `mod_window_x` int(11) NOT NULL DEFAULT 0,
  `mod_window_y` int(11) NOT NULL DEFAULT 0,
  `mod_window_width` int(11) NOT NULL DEFAULT 0,
  `mod_window_height` int(11) NOT NULL DEFAULT 0,
  `nav_window_x` int(11) NOT NULL DEFAULT 0,
  `nav_window_y` int(11) NOT NULL DEFAULT 0,
  `nav_window_width` int(11) NOT NULL DEFAULT 0,
  `nav_window_height` int(11) NOT NULL DEFAULT 0,
  `nav_window_searches_open` tinyint(1) NOT NULL DEFAULT 0,
  `nav_window_results_mode` tinyint(4) NOT NULL DEFAULT 0,
  `discord_version` int(11) NOT NULL DEFAULT 0,
  `discord_sharing` tinyint(3) unsigned NOT NULL DEFAULT 0 COMMENT 'Bits: 1 shown in the Discord status, 2 activity shared, 4 hidden rooms left out, 8 others may join.',
  PRIMARY KEY (`player_id`),
  CONSTRAINT `fk_player_preferences_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What a player chose in the client''s settings.';

CREATE TABLE `extension_player_values` (
  `extension` varchar(32) NOT NULL,
  `player_id` int(11) NOT NULL,
  `name` varchar(64) NOT NULL,
  `value` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`extension`,`player_id`,`name`),
  KEY `idx_extension_player_values_1` (`player_id`),
  CONSTRAINT `fk_extension_player_values_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Numbers a game extension keeps per player.';

CREATE TABLE `player_looks` (
  `player_id` int(11) NOT NULL,
  `slot` tinyint(4) NOT NULL DEFAULT 0,
  `figure` varchar(256) NOT NULL,
  `gender` enum('M','F') NOT NULL DEFAULT 'F',
  PRIMARY KEY (`player_id`,`slot`),
  CONSTRAINT `fk_player_looks_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Saved outfits in the wardrobe.';

CREATE TABLE `name_changes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) NOT NULL,
  `old_name` varchar(32) NOT NULL,
  `new_name` varchar(32) NOT NULL,
  `changed_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_name_changes_1` (`player_id`,`changed_at`),
  CONSTRAINT `fk_name_changes_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Names a player had; a rename checks it.';

CREATE TABLE `player_navigator_sections` (
  `player_id` int(11) NOT NULL,
  `section` varchar(128) NOT NULL,
  `list_mode` enum('list','thumbnails') NOT NULL DEFAULT 'list',
  `collapsed` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`player_id`,`section`),
  CONSTRAINT `fk_player_navigator_sections_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='How a player left a navigator section.';

CREATE TABLE `player_saved_searches` (
  `player_id` int(11) NOT NULL,
  `number` tinyint(3) unsigned NOT NULL,
  `view_code` varchar(64) NOT NULL,
  `search_text` varchar(64) NOT NULL DEFAULT '',
  PRIMARY KEY (`player_id`,`number`),
  UNIQUE KEY `uq_player_saved_searches_1` (`player_id`,`view_code`,`search_text`),
  CONSTRAINT `fk_player_saved_searches_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Navigator searches a player saved, numbered per player: the number is what the client names when it deletes one.';

CREATE TABLE `player_favourite_rooms` (
  `player_id` int(11) NOT NULL,
  `room_id` int(11) NOT NULL,
  PRIMARY KEY (`player_id`,`room_id`),
  KEY `fk_player_favourite_rooms_2` (`room_id`),
  CONSTRAINT `fk_player_favourite_rooms_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_player_favourite_rooms_2` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Rooms a player marked as favourite.';

CREATE TABLE `player_unseen_items` (
  `seq` bigint(20) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) NOT NULL,
  `category` tinyint(4) NOT NULL DEFAULT 0,
  `item_id` int(11) NOT NULL,
  PRIMARY KEY (`seq`),
  UNIQUE KEY `uq_player_unseen_items_1` (`player_id`,`category`,`item_id`),
  CONSTRAINT `fk_player_unseen_items_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The red-dot queue of the inventory.';

CREATE TABLE `player_word_filters` (
  `player_id` int(11) NOT NULL,
  `word` varchar(64) NOT NULL,
  PRIMARY KEY (`player_id`,`word`),
  CONSTRAINT `fk_player_word_filters_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A player''s own filtered words.';

CREATE TABLE `player_ignores` (
  `player_id` int(11) NOT NULL,
  `ignored_id` int(11) NOT NULL,
  PRIMARY KEY (`player_id`,`ignored_id`),
  KEY `fk_player_ignores_2` (`ignored_id`),
  CONSTRAINT `fk_player_ignores_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_player_ignores_2` FOREIGN KEY (`ignored_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Players whose chat a player ignores.';

CREATE TABLE `player_blocks` (
  `player_id` int(11) NOT NULL,
  `blocked_id` int(11) NOT NULL,
  PRIMARY KEY (`player_id`,`blocked_id`),
  KEY `fk_player_blocks_2` (`blocked_id`),
  CONSTRAINT `fk_player_blocks_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_player_blocks_2` FOREIGN KEY (`blocked_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Players a player blocked.';


-- Currencies: balances and the ledger behind them.

CREATE TABLE `player_balances` (
  `player_id` int(11) NOT NULL,
  `currency` int(11) NOT NULL,
  `amount` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`player_id`,`currency`),
  KEY `ix_player_balances_1` (`currency`,`amount`),
  CONSTRAINT `fk_player_balances_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='One row per player and currency. Credits are currency -1, the others are the currencies of the content; a missing row is a balance of 0.';

CREATE TABLE `economy_entries` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `entry_key` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Names the event behind the change; a key is written once.',
  `player_id` int(11) NOT NULL,
  `currency` int(11) NOT NULL,
  `amount` int(11) NOT NULL,
  `balance_after` int(11) NOT NULL,
  `cause` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Dotted, area first: market.buy, shop.refund.',
  `actor_id` int(11) DEFAULT NULL COMMENT 'Another player who caused the change.',
  `furni_id` int(11) DEFAULT NULL,
  `note` varchar(160) NOT NULL DEFAULT '',
  `created_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_economy_entries_1` (`entry_key`),
  KEY `ix_economy_entries_1` (`player_id`,`created_at`),
  KEY `ix_economy_entries_2` (`cause`,`created_at`),
  KEY `ix_economy_entries_3` (`actor_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Append-only ledger of every balance change. Two triggers refuse updates and deletes.';

DELIMITER ;;
CREATE TRIGGER `economy_entries_no_update` BEFORE UPDATE ON `economy_entries` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'economy_entries is append-only';;
DELIMITER ;

DELIMITER ;;
CREATE TRIGGER `economy_entries_no_delete` BEFORE DELETE ON `economy_entries` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'economy_entries is append-only';;
DELIMITER ;


-- Friends and messenger

CREATE TABLE `friendships` (
  `player_id` int(11) NOT NULL,
  `friend_id` int(11) NOT NULL,
  `relation` tinyint(4) NOT NULL DEFAULT 0,
  `category_id` int(11) DEFAULT NULL,
  `friends_since` int(10) unsigned NOT NULL,
  PRIMARY KEY (`player_id`,`friend_id`),
  KEY `ix_friendships_1` (`friend_id`),
  KEY `fk_friendships_3` (`category_id`),
  CONSTRAINT `fk_friendships_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_friendships_2` FOREIGN KEY (`friend_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_friendships_3` FOREIGN KEY (`category_id`) REFERENCES `friend_categories` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='One side of a friendship.';

CREATE TABLE `friend_categories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) NOT NULL,
  `name` varchar(25) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_friend_categories_1` (`player_id`,`id`),
  CONSTRAINT `fk_friend_categories_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A player''s friend list folders.';

CREATE TABLE `friend_requests` (
  `sender_id` int(11) NOT NULL,
  `target_id` int(11) NOT NULL,
  `sent_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`target_id`,`sender_id`),
  KEY `fk_friend_requests_1` (`sender_id`),
  CONSTRAINT `fk_friend_requests_1` FOREIGN KEY (`sender_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_friend_requests_2` FOREIGN KEY (`target_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Friend requests waiting for an answer.';

CREATE TABLE `direct_messages` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sender_id` int(11) NOT NULL,
  `target_id` int(11) NOT NULL,
  `message_code` varchar(24) NOT NULL,
  `message` varchar(500) NOT NULL DEFAULT '',
  `icon_id` int(11) DEFAULT NULL,
  `sent_at` int(10) unsigned NOT NULL,
  `delivered_at` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_direct_messages_1` (`sender_id`,`target_id`,`id`),
  KEY `ix_direct_messages_2` (`message_code`),
  KEY `ix_direct_messages_3` (`target_id`,`delivered_at`,`id`),
  KEY `fk_direct_messages_3` (`icon_id`),
  CONSTRAINT `fk_direct_messages_1` FOREIGN KEY (`sender_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_direct_messages_2` FOREIGN KEY (`target_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_direct_messages_3` FOREIGN KEY (`icon_id`) REFERENCES `icons` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Every messenger message; one without a delivery time waits for its recipient to log in.';


-- Wearables and chat looks: badges, effects, clothing, chat bubbles and chat icons.

CREATE TABLE `player_badges` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) NOT NULL,
  `badge_code` varchar(32) NOT NULL,
  `slot` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_player_badges_1` (`player_id`,`badge_code`),
  KEY `ix_player_badges_1` (`badge_code`,`player_id`),
  CONSTRAINT `fk_player_badges_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Badges a player owns and which are worn.';

CREATE TABLE `player_effects` (
  `player_id` int(11) NOT NULL,
  `effect_id` int(11) NOT NULL,
  `duration_seconds` int(11) NOT NULL DEFAULT 86400,
  `activated_at` int(10) unsigned DEFAULT NULL,
  `remaining` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`player_id`,`effect_id`),
  CONSTRAINT `fk_player_effects_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Avatar effects a player owns.';

CREATE TABLE `clothing` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(75) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Clothing a clothing voucher furni unlocks.';

CREATE TABLE `clothing_sets` (
  `clothing_id` int(11) NOT NULL,
  `position` tinyint(4) NOT NULL DEFAULT 0,
  `set_id` int(11) NOT NULL,
  PRIMARY KEY (`clothing_id`,`position`),
  CONSTRAINT `fk_clothing_sets_1` FOREIGN KEY (`clothing_id`) REFERENCES `clothing` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The figure set ids of a clothing entry.';

CREATE TABLE `player_clothing` (
  `player_id` int(11) NOT NULL,
  `clothing_id` int(11) NOT NULL,
  PRIMARY KEY (`player_id`,`clothing_id`),
  KEY `fk_player_clothing_2` (`clothing_id`),
  CONSTRAINT `fk_player_clothing_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_player_clothing_2` FOREIGN KEY (`clothing_id`) REFERENCES `clothing` (`id`) ON DELETE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Clothing sets a player unlocked.';

CREATE TABLE `featured_looks` (
  `figure` varchar(255) NOT NULL,
  `gender` enum('M','F') NOT NULL DEFAULT 'M',
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`figure`,`gender`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Looks the outfit picker suggests. A look is its own key and the list has no order: every reader draws from it at random.';

CREATE TABLE `currencies` (
  `id` int(11) NOT NULL,
  `code` varchar(32) NOT NULL,
  `name` varchar(40) NOT NULL,
  `points` tinyint(1) NOT NULL DEFAULT 1,
  `role` enum('credits','points','premium','other') NOT NULL DEFAULT 'other',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_currencies_1` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The currencies by id: -1 is the credits, the others are activity points a player holds when points is set. The name is what players read; the role says which one the server pays points and premium rewards in.';

CREATE TABLE `display_names` (
  `code` varchar(32) NOT NULL,
  `name` varchar(40) NOT NULL,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The names players read for the build pass (build_pass) and the club (club).';

CREATE TABLE `effect_roles` (
  `role` varchar(64) NOT NULL,
  `effect_id` int(11) NOT NULL,
  PRIMARY KEY (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The avatar effect a server role puts on a unit: team colours, freeze, teleport, water, cannon, riding and the like. A role with no row is no effect.';

CREATE TABLE `chat_bubbles` (
  `id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL DEFAULT '',
  `permission_code` varchar(255) NOT NULL DEFAULT '',
  `overridable` tinyint(1) NOT NULL DEFAULT 1,
  `triggers_talking_furni` tinyint(1) NOT NULL DEFAULT 0,
  `offer_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_chat_bubbles_1` (`offer_id`),
  CONSTRAINT `fk_chat_bubbles_1` FOREIGN KEY (`offer_id`) REFERENCES `catalogue_offers` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chat bubble styles by id; an id with no row is the plain bubble.';

CREATE TABLE `player_chat_bubbles` (
  `player_id` int(11) NOT NULL,
  `bubble_id` int(11) NOT NULL,
  `acquired_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`player_id`,`bubble_id`),
  KEY `ix_player_chat_bubbles_1` (`bubble_id`),
  CONSTRAINT `fk_player_chat_bubbles_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_player_chat_bubbles_2` FOREIGN KEY (`bubble_id`) REFERENCES `chat_bubbles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chat bubbles a player owns.';

CREATE TABLE `icons` (
  `id` int(11) NOT NULL,
  `collection_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `available` tinyint(1) NOT NULL DEFAULT 1,
  `owned_by_default` tinyint(1) NOT NULL DEFAULT 0,
  `price_credits` int(11) NOT NULL DEFAULT 0,
  `price_currency` int(11) NOT NULL DEFAULT 0,
  `currency` int(11) NOT NULL DEFAULT 0,
  `sort_order` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `ix_icons_1` (`collection_id`),
  CONSTRAINT `fk_icons_1` FOREIGN KEY (`collection_id`) REFERENCES `icon_collections` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chat icons a player can show in a room or a message.';

CREATE TABLE `icon_collections` (
  `id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `reward_id` int(11) NOT NULL DEFAULT 0,
  `price_credits` int(11) NOT NULL DEFAULT 0,
  `price_currency` int(11) NOT NULL DEFAULT 0,
  `currency` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sets of chat icons on sale.';

CREATE TABLE `player_icons` (
  `player_id` int(11) NOT NULL,
  `icon_id` int(11) NOT NULL,
  `state` tinyint(4) NOT NULL DEFAULT 2 CHECK (`state` in (1,2,3)),
  `unseen` tinyint(1) NOT NULL DEFAULT 0,
  `last_used_ms` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`player_id`,`icon_id`),
  KEY `ix_player_icons_1` (`player_id`,`last_used_ms`),
  KEY `ix_player_icons_2` (`icon_id`),
  CONSTRAINT `fk_player_icons_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_player_icons_2` FOREIGN KEY (`icon_id`) REFERENCES `icons` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chat icons a player owns.';


-- Furni

CREATE TABLE `furni_definitions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(70) NOT NULL,
  `display_name` varchar(56) NOT NULL DEFAULT '',
  `sprite_id` int(11) NOT NULL DEFAULT 0,
  `kind` char(1) NOT NULL DEFAULT 's',
  `width` tinyint(4) NOT NULL DEFAULT 1,
  `length` tinyint(4) NOT NULL DEFAULT 1,
  `stack_height` double NOT NULL DEFAULT 0,
  `can_stack` tinyint(1) NOT NULL DEFAULT 1,
  `can_sit` tinyint(1) NOT NULL DEFAULT 0,
  `can_lay` tinyint(1) NOT NULL DEFAULT 0,
  `can_walk` tinyint(1) NOT NULL DEFAULT 0,
  `can_gift` tinyint(1) NOT NULL DEFAULT 1,
  `can_trade` tinyint(1) NOT NULL DEFAULT 1,
  `can_recycle` tinyint(1) NOT NULL DEFAULT 0,
  `can_sell` tinyint(1) NOT NULL DEFAULT 0,
  `stacks_in_inventory` tinyint(1) NOT NULL DEFAULT 1,
  `behaviour` varchar(64) NOT NULL,
  `state_count` smallint(6) NOT NULL DEFAULT 1,
  `behaviour_params` varchar(256) NOT NULL DEFAULT '',
  `effect_male` int(11) NOT NULL DEFAULT 0,
  `effect_female` int(11) NOT NULL DEFAULT 0,
  `clothing_on_walk` varchar(255) NOT NULL DEFAULT '',
  `direction_mask` tinyint(3) unsigned NOT NULL DEFAULT 0,
  `usage_policy` tinyint(3) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_furni_definitions_1` (`code`),
  KEY `ix_furni_definitions_1` (`sprite_id`),
  KEY `ix_furni_definitions_2` (`behaviour`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A kind of furni. A pack brings them.';

CREATE TABLE `furni` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) DEFAULT NULL,
  `room_id` int(11) DEFAULT NULL,
  `definition_id` int(11) NOT NULL,
  `x` smallint(6) NOT NULL DEFAULT 0,
  `y` smallint(6) NOT NULL DEFAULT 0,
  `z` double NOT NULL DEFAULT 0,
  `direction` tinyint(4) NOT NULL DEFAULT 0,
  `wall_position` varchar(32) NOT NULL DEFAULT '',
  `state` varchar(1024) NOT NULL DEFAULT '',
  `limited_number` int(11) NOT NULL DEFAULT 0,
  `limited_total` int(11) NOT NULL DEFAULT 0,
  `guild_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_furni_1` (`player_id`,`room_id`),
  KEY `ix_furni_2` (`room_id`,`definition_id`),
  KEY `fk_furni_3` (`definition_id`),
  KEY `fk_furni_4` (`guild_id`),
  CONSTRAINT `fk_furni_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_furni_2` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_furni_3` FOREIGN KEY (`definition_id`) REFERENCES `furni_definitions` (`id`) ON DELETE NO ACTION,
  CONSTRAINT `fk_furni_4` FOREIGN KEY (`guild_id`) REFERENCES `guilds` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Every furni in the game: placed, in an inventory, on sale or in a gift.';

CREATE TABLE `furni_handitems` (
  `definition_id` int(11) NOT NULL,
  `position` tinyint(4) NOT NULL DEFAULT 0,
  `handitem_id` int(11) NOT NULL,
  PRIMARY KEY (`definition_id`,`position`),
  CONSTRAINT `fk_furni_handitems_1` FOREIGN KEY (`definition_id`) REFERENCES `furni_definitions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What a vending furni hands out.';

CREATE TABLE `furni_labels` (
  `furni_id` int(11) NOT NULL,
  `text` text NOT NULL,
  `color` int(11) NOT NULL DEFAULT 4278912,
  `visibility` tinyint(4) NOT NULL DEFAULT 1,
  `always_open` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`furni_id`),
  CONSTRAINT `fk_furni_labels_1` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sticker and label text on a furni.';

CREATE TABLE `furni_state_heights` (
  `definition_id` int(11) NOT NULL,
  `state` tinyint(4) NOT NULL DEFAULT 0,
  `height` double NOT NULL,
  PRIMARY KEY (`definition_id`,`state`),
  CONSTRAINT `fk_furni_state_heights_1` FOREIGN KEY (`definition_id`) REFERENCES `furni_definitions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Height per state of a furni whose height follows its state.';

CREATE TABLE `teleport_pairs` (
  `first_furni_id` int(11) NOT NULL,
  `second_furni_id` int(11) NOT NULL,
  PRIMARY KEY (`first_furni_id`),
  UNIQUE KEY `uq_teleport_pairs_1` (`second_furni_id`),
  CONSTRAINT `fk_teleport_pairs_1` FOREIGN KEY (`first_furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_teleport_pairs_2` FOREIGN KEY (`second_furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Two teleports that lead to each other.';

CREATE TABLE `teleport_targets` (
  `furni_id` int(11) NOT NULL,
  `target_furni_id` int(11) NOT NULL,
  PRIMARY KEY (`furni_id`),
  KEY `fk_teleport_targets_2` (`target_furni_id`),
  CONSTRAINT `fk_teleport_targets_1` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_teleport_targets_2` FOREIGN KEY (`target_furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A teleport sent elsewhere than its pair by a ~teleport.target_id write; one way only.';

CREATE TABLE `build_pass_furni` (
  `furni_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `room_id` int(11) DEFAULT NULL,
  `client_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`furni_id`),
  UNIQUE KEY `uq_build_pass_furni_1` (`client_id`),
  KEY `ix_build_pass_furni_1` (`player_id`),
  KEY `ix_build_pass_furni_2` (`room_id`),
  CONSTRAINT `fk_build_pass_furni_1` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_build_pass_furni_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Build Pass furni: the id the client knows and the placement the membership counts.';

CREATE TABLE `plant_kinds` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `furni_code` varchar(70) NOT NULL,
  `grow_count` int(11) NOT NULL DEFAULT 1,
  `death_count` int(11) NOT NULL DEFAULT 2,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_plant_kinds_1` (`furni_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='How a plant furni grows.';

CREATE TABLE `plants` (
  `furni_id` int(11) NOT NULL,
  `count` int(11) NOT NULL DEFAULT 0,
  `last_watered_at` int(10) unsigned DEFAULT NULL,
  `state` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`furni_id`),
  CONSTRAINT `fk_plants_1` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A placed plant''s growth.';

CREATE TABLE `crackables` (
  `definition_id` int(11) NOT NULL,
  `furni_code` varchar(255) NOT NULL,
  `hits_needed` int(11) NOT NULL DEFAULT 0,
  `hit_achievement` varchar(64) NOT NULL,
  `cracked_achievement` varchar(64) NOT NULL,
  `required_effect` int(11) NOT NULL DEFAULT 0,
  `subscription_kind` enum('club','vip','build_pass','build_pass_addon') DEFAULT NULL,
  `subscription_days` int(11) DEFAULT NULL,
  `no_prize_chance` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`definition_id`),
  CONSTRAINT `fk_crackables_1` FOREIGN KEY (`definition_id`) REFERENCES `furni_definitions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Furni that crack open after a number of hits.';

CREATE TABLE `crackable_prizes` (
  `definition_id` int(11) NOT NULL,
  `prize_definition_id` int(11) NOT NULL,
  `chance` int(11) NOT NULL DEFAULT 100,
  PRIMARY KEY (`definition_id`,`prize_definition_id`),
  KEY `fk_crackable_prizes_2` (`prize_definition_id`),
  CONSTRAINT `fk_crackable_prizes_1` FOREIGN KEY (`definition_id`) REFERENCES `crackables` (`definition_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_crackable_prizes_2` FOREIGN KEY (`prize_definition_id`) REFERENCES `furni_definitions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What a crackable can give and how likely.';

CREATE TABLE `mystery_box_prizes` (
  `colour` varchar(32) NOT NULL DEFAULT '',
  `offer_id` int(11) NOT NULL,
  `weight` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`colour`,`offer_id`),
  KEY `fk_mystery_box_prizes_1` (`offer_id`),
  CONSTRAINT `fk_mystery_box_prizes_1` FOREIGN KEY (`offer_id`) REFERENCES `catalogue_offers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What a mystery box of a colour can give; no colour means every colour, weight 0 never comes out.';

CREATE TABLE `soundtracks` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(32) NOT NULL,
  `name` varchar(100) NOT NULL,
  `author` varchar(50) NOT NULL,
  `track` text NOT NULL,
  `length_seconds` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_soundtracks_1` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Music discs.';

CREATE TABLE `sound_machine_songs` (
  `furni_id` int(11) NOT NULL,
  `slot` smallint(6) NOT NULL,
  `soundtrack_id` int(11) NOT NULL,
  PRIMARY KEY (`furni_id`,`slot`),
  KEY `ix_sound_machine_songs_1` (`soundtrack_id`),
  CONSTRAINT `fk_sound_machine_songs_1` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_sound_machine_songs_2` FOREIGN KEY (`soundtrack_id`) REFERENCES `soundtracks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The playlist of a sound machine furni.';

CREATE TABLE `youtube_playlists` (
  `position` int(11) NOT NULL,
  `playlist_code` varchar(64) NOT NULL,
  PRIMARY KEY (`position`),
  UNIQUE KEY `uq_youtube_playlists_1` (`playlist_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The playlists every television furni shows, in order.';

CREATE TABLE `crafting_recipes` (
  `id` int(11) NOT NULL,
  `altar_definition_id` int(11) NOT NULL COMMENT 'The crafting table furni that makes it.',
  `product_name` varchar(64) NOT NULL,
  `reward_definition_id` int(11) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `achievement_code` varchar(255) NOT NULL DEFAULT '',
  `secret` tinyint(1) NOT NULL DEFAULT 0,
  `stock_left` int(11) DEFAULT NULL COMMENT 'Counts down with every craft; NULL is no limit.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_crafting_recipes_1` (`product_name`),
  KEY `fk_crafting_recipes_1` (`reward_definition_id`),
  KEY `fk_crafting_recipes_2` (`altar_definition_id`),
  CONSTRAINT `fk_crafting_recipes_1` FOREIGN KEY (`reward_definition_id`) REFERENCES `furni_definitions` (`id`) ON DELETE NO ACTION,
  CONSTRAINT `fk_crafting_recipes_2` FOREIGN KEY (`altar_definition_id`) REFERENCES `furni_definitions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What each crafting table furni makes.';

CREATE TABLE `crafting_ingredients` (
  `recipe_id` int(11) NOT NULL,
  `definition_id` int(11) NOT NULL,
  `amount` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`recipe_id`,`definition_id`),
  KEY `fk_crafting_ingredients_2` (`definition_id`),
  CONSTRAINT `fk_crafting_ingredients_1` FOREIGN KEY (`recipe_id`) REFERENCES `crafting_recipes` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_crafting_ingredients_2` FOREIGN KEY (`definition_id`) REFERENCES `furni_definitions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What a recipe takes.';

CREATE TABLE `player_recipes` (
  `player_id` int(11) NOT NULL,
  `recipe_id` int(11) NOT NULL,
  PRIMARY KEY (`player_id`,`recipe_id`),
  KEY `fk_player_recipes_2` (`recipe_id`),
  CONSTRAINT `fk_player_recipes_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_player_recipes_2` FOREIGN KEY (`recipe_id`) REFERENCES `crafting_recipes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Secret recipes a player found.';

CREATE TABLE `recycler_boxes` (
  `furni_id` int(11) NOT NULL,
  `prize_definition_id` int(11) DEFAULT NULL,
  `chat_bubble_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`furni_id`),
  KEY `fk_recycler_boxes_2` (`prize_definition_id`),
  KEY `fk_recycler_boxes_3` (`chat_bubble_id`),
  CONSTRAINT `fk_recycler_boxes_1` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_recycler_boxes_2` FOREIGN KEY (`prize_definition_id`) REFERENCES `furni_definitions` (`id`) ON DELETE NO ACTION,
  CONSTRAINT `fk_recycler_boxes_3` FOREIGN KEY (`chat_bubble_id`) REFERENCES `chat_bubbles` (`id`) ON DELETE NO ACTION,
  CONSTRAINT `ck_recycler_boxes_prize` CHECK ((`prize_definition_id` IS NULL) <> (`chat_bubble_id` IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What an unopened recycler box holds.';

CREATE TABLE `recycler_prizes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `rarity` int(11) NOT NULL DEFAULT 0,
  `definition_id` int(11) DEFAULT NULL,
  `furni_code` varchar(70) NOT NULL DEFAULT '',
  `chat_bubble_id` int(11) DEFAULT NULL,
  `sort_order` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `ix_recycler_prizes_1` (`rarity`,`sort_order`),
  KEY `fk_recycler_prizes_1` (`definition_id`),
  KEY `fk_recycler_prizes_2` (`chat_bubble_id`),
  CONSTRAINT `fk_recycler_prizes_1` FOREIGN KEY (`definition_id`) REFERENCES `furni_definitions` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_recycler_prizes_2` FOREIGN KEY (`chat_bubble_id`) REFERENCES `chat_bubbles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What the recycler can give per rarity.';


-- Rooms

CREATE TABLE `room_models` (
  `name` varchar(100) NOT NULL,
  `door_x` int(11) NOT NULL DEFAULT 0,
  `door_y` int(11) NOT NULL DEFAULT 0,
  `door_direction` tinyint(4) NOT NULL DEFAULT 2,
  `heightmap` text NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The shared room shapes; a pack brings them.';

CREATE TABLE `room_categories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `min_rank` int(11) NOT NULL DEFAULT 0,
  `caption_key` varchar(32) NOT NULL DEFAULT 'caption_save',
  `caption` varchar(100) NOT NULL,
  `name` varchar(100) NOT NULL DEFAULT '',
  `visible` tinyint(1) NOT NULL DEFAULT 1,
  `automatic` tinyint(1) NOT NULL DEFAULT 0,
  `automatic_key` varchar(32) NOT NULL DEFAULT 'NONE',
  `staff_only` tinyint(1) NOT NULL DEFAULT 0,
  `max_players` int(11) NOT NULL DEFAULT 100,
  `is_public` tinyint(1) NOT NULL DEFAULT 0,
  `list_mode` tinyint(4) NOT NULL DEFAULT 0,
  `sort_order` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Categories of the room settings and navigator.';

CREATE TABLE `rooms` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `owner_id` int(11) DEFAULT NULL,
  `name` varchar(50) NOT NULL DEFAULT '',
  `description` varchar(512) NOT NULL DEFAULT '',
  `model_name` varchar(20) NOT NULL DEFAULT 'model_a',
  `door_mode` enum('open','locked','password','invisible') NOT NULL DEFAULT 'open',
  `door_secret` varchar(96) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT 'The door password, salted and hashed (k1:rounds:salt:key); empty without one.',
  `max_players` int(11) NOT NULL DEFAULT 25,
  `player_count` int(11) NOT NULL DEFAULT 0,
  `guild_id` int(11) DEFAULT NULL,
  `category_id` int(11) NOT NULL DEFAULT 1,
  `score` int(11) NOT NULL DEFAULT 0,
  `floor_paper` varchar(5) NOT NULL DEFAULT '0.0',
  `wall_paper` varchar(5) NOT NULL DEFAULT '0.0',
  `landscape_paper` varchar(5) NOT NULL DEFAULT '0.0',
  `wall_thickness` tinyint(4) NOT NULL DEFAULT 0,
  `floor_thickness` tinyint(4) NOT NULL DEFAULT 0,
  `wall_height` tinyint(4) NOT NULL DEFAULT -1,
  `hide_walls` tinyint(1) NOT NULL DEFAULT 0,
  `is_public` tinyint(1) NOT NULL DEFAULT 0,
  `is_staff_picked` tinyint(1) NOT NULL DEFAULT 0,
  `pets_allowed` tinyint(1) NOT NULL DEFAULT 0,
  `pets_may_eat` tinyint(1) NOT NULL DEFAULT 0,
  `pets_muted` tinyint(1) NOT NULL DEFAULT 0,
  `walk_through` tinyint(1) NOT NULL DEFAULT 1,
  `leave_on_door_tile` tinyint(1) NOT NULL DEFAULT 1,
  `move_diagonally` tinyint(1) NOT NULL DEFAULT 1,
  `chat_protection` tinyint(4) NOT NULL DEFAULT 1,
  `who_can_mute` tinyint(4) NOT NULL DEFAULT 0,
  `who_can_kick` tinyint(4) NOT NULL DEFAULT 0,
  `who_can_ban` tinyint(4) NOT NULL DEFAULT 0,
  `trade_mode` tinyint(4) NOT NULL DEFAULT 2,
  `idle_sleep` tinyint(1) NOT NULL DEFAULT 0,
  `idle_sleep_seconds` int(11) NOT NULL DEFAULT 0,
  `idle_kick` tinyint(1) NOT NULL DEFAULT 1,
  `idle_kick_seconds` int(11) NOT NULL DEFAULT 900,
  `build_pass_locked` tinyint(1) NOT NULL DEFAULT 0,
  `build_pass_door_mode` enum('open','locked','password','invisible') NOT NULL DEFAULT 'open',
  `is_promoted` tinyint(1) NOT NULL DEFAULT 0,
  `has_custom_floor` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` int(10) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `ix_rooms_1` (`owner_id`),
  KEY `ix_rooms_2` (`guild_id`),
  KEY `ix_rooms_3` (`category_id`),
  KEY `ix_rooms_4` (`is_public`,`is_staff_picked`),
  KEY `ix_rooms_5` (`player_count`),
  KEY `ix_rooms_6` (`name`),
  CONSTRAINT `fk_rooms_1` FOREIGN KEY (`owner_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_rooms_2` FOREIGN KEY (`guild_id`) REFERENCES `guilds` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_rooms_3` FOREIGN KEY (`category_id`) REFERENCES `room_categories` (`id`) ON DELETE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A room and its settings.';

CREATE TABLE `room_floor_plans` (
  `room_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `door_x` int(11) NOT NULL DEFAULT 0,
  `door_y` int(11) NOT NULL DEFAULT 0,
  `door_direction` tinyint(4) NOT NULL DEFAULT 2,
  `heightmap` text NOT NULL,
  PRIMARY KEY (`room_id`),
  CONSTRAINT `fk_room_floor_plans_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A room''s own floor, from the floor plan editor; replaces its model.';

CREATE TABLE `room_rights` (
  `room_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  PRIMARY KEY (`room_id`,`player_id`),
  KEY `ix_room_rights_1` (`player_id`,`room_id`),
  CONSTRAINT `fk_room_rights_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_room_rights_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Players with rights in a room.';

CREATE TABLE `room_bans` (
  `room_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `ends_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`room_id`,`player_id`),
  KEY `ix_room_bans_1` (`ends_at`),
  KEY `fk_room_bans_2` (`player_id`),
  CONSTRAINT `fk_room_bans_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_room_bans_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Players banned from a room until a moment.';

CREATE TABLE `room_mutes` (
  `room_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `ends_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`room_id`,`player_id`),
  KEY `ix_room_mutes_1` (`ends_at`),
  KEY `fk_room_mutes_2` (`player_id`),
  CONSTRAINT `fk_room_mutes_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_room_mutes_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Players muted in a room until a moment.';

CREATE TABLE `room_tags` (
  `room_id` int(11) NOT NULL,
  `position` tinyint(4) NOT NULL DEFAULT 0,
  `tag` varchar(64) NOT NULL,
  PRIMARY KEY (`room_id`,`position`),
  KEY `ix_room_tags_1` (`tag`),
  CONSTRAINT `fk_room_tags_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A room''s navigator tags.';

CREATE TABLE `room_votes` (
  `player_id` int(11) NOT NULL,
  `room_id` int(11) NOT NULL,
  PRIMARY KEY (`player_id`,`room_id`),
  KEY `ix_room_votes_1` (`room_id`),
  CONSTRAINT `fk_room_votes_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_room_votes_2` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Who voted for a room.';

CREATE TABLE `room_visits` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `room_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `entered_at` int(10) unsigned NOT NULL,
  `left_at` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_room_visits_1` (`room_id`,`entered_at`),
  KEY `ix_room_visits_2` (`player_id`,`entered_at`),
  CONSTRAINT `fk_room_visits_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_room_visits_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Who was in which room when; moderation and staff pages read it.';

CREATE TABLE `room_word_filters` (
  `room_id` int(11) NOT NULL,
  `word` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`room_id`,`word`),
  CONSTRAINT `fk_room_word_filters_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Words a room filters.';

CREATE TABLE `room_dimmer_presets` (
  `room_id` int(11) NOT NULL,
  `position` tinyint(4) NOT NULL DEFAULT 0,
  `preset` tinyint(4) NOT NULL DEFAULT 0,
  `selected` tinyint(1) NOT NULL DEFAULT 0,
  `background_only` tinyint(1) NOT NULL DEFAULT 0,
  `color` char(7) NOT NULL DEFAULT '#000000',
  `brightness` smallint(6) NOT NULL DEFAULT 255,
  PRIMARY KEY (`room_id`,`position`),
  CONSTRAINT `fk_room_dimmer_presets_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The three moodlight presets of a room.';

CREATE TABLE `room_promotions` (
  `room_id` int(11) NOT NULL,
  `title` varchar(127) NOT NULL,
  `description` varchar(1024) NOT NULL,
  `category` int(11) NOT NULL DEFAULT 0,
  `starts_at` int(10) unsigned DEFAULT NULL,
  `ends_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`room_id`),
  KEY `ix_room_promotions_1` (`ends_at`),
  CONSTRAINT `fk_room_promotions_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A room''s running ad in the navigator.';

CREATE TABLE `room_raid_settings` (
  `room_id` int(11) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 0,
  `sensitivity` int(11) NOT NULL DEFAULT 1,
  `action` int(11) NOT NULL DEFAULT 1,
  `ban_seconds` int(11) NOT NULL DEFAULT 900,
  `guard_enabled` tinyint(1) NOT NULL DEFAULT 0,
  `guard_seconds` int(11) NOT NULL DEFAULT 900,
  `guard_sensitivity` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`room_id`),
  CONSTRAINT `fk_room_raid_settings_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A room''s raid protection.';

CREATE TABLE `room_playlist_discs` (
  `room_id` int(11) NOT NULL,
  `furni_id` int(11) NOT NULL,
  PRIMARY KEY (`furni_id`),
  KEY `ix_room_playlist_discs_1` (`room_id`),
  CONSTRAINT `fk_room_playlist_discs_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_room_playlist_discs_2` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Discs on a room''s jukebox. The furni row has no room and no place while it is here.';

CREATE TABLE `public_room_categories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(32) NOT NULL,
  `show_image` tinyint(1) NOT NULL DEFAULT 0,
  `visible` tinyint(1) NOT NULL DEFAULT 1,
  `sort_order` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sections of the public rooms list.';

CREATE TABLE `public_rooms` (
  `category_id` int(11) NOT NULL,
  `room_id` int(11) NOT NULL,
  `visible` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`category_id`,`room_id`),
  KEY `fk_public_rooms_2` (`room_id`),
  CONSTRAINT `fk_public_rooms_1` FOREIGN KEY (`category_id`) REFERENCES `public_room_categories` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_public_rooms_2` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Rooms in the public rooms list.';

CREATE TABLE `camera_filters` (
  `name` varchar(64) NOT NULL,
  `kind` enum('matrix','overlay','border') NOT NULL,
  `blend` enum('normal','add','multiply','screen','overlay','hardlight','darken','lighten') DEFAULT NULL,
  `matrix` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A filter or border a photo frame may choose. A matrix is its rows of five numbers split by |; an overlay carries its blend mode.';

CREATE TABLE `camera_libraries` (
  `name` varchar(64) NOT NULL,
  `base` tinyint(1) NOT NULL DEFAULT 0,
  `shades` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Figure libraries a photo draws from. base ones are always loaded; shades lists grey levels drawn lighter as from:to,from:to.';

CREATE TABLE `camera_photos` (
  `id` char(36) NOT NULL,
  `player_id` int(11) DEFAULT NULL,
  `room_id` int(11) DEFAULT NULL,
  `created_at_ms` bigint(20) NOT NULL,
  `purchases` int(11) NOT NULL DEFAULT 0,
  `published` tinyint(1) NOT NULL DEFAULT 0,
  `published_at` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_camera_photos_1` (`player_id`),
  KEY `ix_camera_photos_2` (`published`,`published_at`),
  CONSTRAINT `fk_camera_photos_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Photos players took. Files live on disk under this id.';


-- Catalogue and trade

CREATE TABLE `catalogue_layouts` (
  `code` varchar(32) NOT NULL,
  `wire_code` varchar(32) NOT NULL DEFAULT '',
  `kind` enum('fields','club_buy','build_pass','folder') NOT NULL DEFAULT 'fields',
  `role` enum('none','club_gift','room_ads','room_bundle') NOT NULL DEFAULT 'none',
  `membership_window` enum('club','build_pass','build_pass_addons') NOT NULL DEFAULT 'club',
  `images` varchar(128) NOT NULL DEFAULT '',
  `texts` varchar(128) NOT NULL DEFAULT '',
  `lists_featured` tinyint(1) NOT NULL DEFAULT 0,
  `needs` enum('none','recycler','marketplace') NOT NULL DEFAULT 'none',
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The page layouts a catalogue can use: what a page of that layout sends and what the server does with it. Filled by the content. images and texts list page fields (header, teaser, special / one, two, details, teaser), - for an empty one, name~sep to cut a field at sep.';

CREATE TABLE `catalogue_pages` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `catalogue` enum('normal','build_pass') NOT NULL DEFAULT 'normal',
  `parent_id` int(11) DEFAULT NULL,
  `caption` varchar(128) NOT NULL,
  `code` varchar(128) NOT NULL DEFAULT '',
  `layout` varchar(32) NOT NULL,
  `icon_color` int(11) NOT NULL DEFAULT 1,
  `icon_image` int(11) NOT NULL DEFAULT 1,
  `min_rank` int(11) NOT NULL DEFAULT 0,
  `sort_order` int(11) NOT NULL DEFAULT 1,
  `visible` tinyint(1) NOT NULL DEFAULT 1,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `club_only` tinyint(1) NOT NULL DEFAULT 0,
  `vip_only` tinyint(1) NOT NULL DEFAULT 0,
  `headline` varchar(1024) NOT NULL DEFAULT '',
  `teaser` varchar(64) NOT NULL DEFAULT '',
  `special_image` varchar(2048) NOT NULL DEFAULT '',
  `text_1` mediumtext DEFAULT NULL,
  `text_2` mediumtext DEFAULT NULL,
  `text_details` mediumtext DEFAULT NULL,
  `text_teaser` mediumtext DEFAULT NULL,
  `room_id` int(11) DEFAULT NULL,
  `ends_at` int(10) unsigned DEFAULT NULL,
  `expiry_image` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_catalogue_pages_1` (`catalogue`,`parent_id`,`sort_order`),
  KEY `fk_catalogue_pages_1` (`parent_id`),
  KEY `fk_catalogue_pages_2` (`room_id`),
  CONSTRAINT `fk_catalogue_pages_1` FOREIGN KEY (`parent_id`) REFERENCES `catalogue_pages` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_catalogue_pages_2` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A page in one of the two catalogues.';

CREATE TABLE `catalogue_page_includes` (
  `page_id` int(11) NOT NULL,
  `position` tinyint(4) NOT NULL DEFAULT 0,
  `included_page_id` int(11) NOT NULL,
  PRIMARY KEY (`page_id`,`position`),
  KEY `fk_catalogue_page_includes_2` (`included_page_id`),
  CONSTRAINT `fk_catalogue_page_includes_1` FOREIGN KEY (`page_id`) REFERENCES `catalogue_pages` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_catalogue_page_includes_2` FOREIGN KEY (`included_page_id`) REFERENCES `catalogue_pages` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Pages shown inside another page.';

CREATE TABLE `catalogue_offers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `page_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `price_credits` int(11) NOT NULL DEFAULT 3,
  `price_currency` int(11) NOT NULL DEFAULT 0,
  `currency` int(11) NOT NULL DEFAULT 0,
  `amount` int(11) NOT NULL DEFAULT 1,
  `limited_total` int(11) NOT NULL DEFAULT 0,
  `limited_sold` int(11) NOT NULL DEFAULT 0,
  `limited_order` enum('random','ascending','descending') NOT NULL DEFAULT 'random' COMMENT 'How a limited edition hands out its numbers.',
  `sort_order` int(11) NOT NULL DEFAULT 1,
  `client_offer_id` int(11) NOT NULL DEFAULT -1,
  `preset_state` varchar(500) NOT NULL DEFAULT '',
  `has_offer` tinyint(1) NOT NULL DEFAULT 1,
  `club_only` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `ix_catalogue_offers_1` (`page_id`),
  KEY `ix_catalogue_offers_2` (`name`),
  CONSTRAINT `fk_catalogue_offers_1` FOREIGN KEY (`page_id`) REFERENCES `catalogue_pages` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Something a page sells; a line in the client''s buy grid.';

CREATE TABLE `catalogue_offer_items` (
  `offer_id` int(11) NOT NULL,
  `position` tinyint(4) NOT NULL DEFAULT 0,
  `definition_id` int(11) NOT NULL,
  `amount` int(11) DEFAULT NULL,
  PRIMARY KEY (`offer_id`,`position`),
  KEY `ix_catalogue_offer_items_1` (`definition_id`),
  CONSTRAINT `fk_catalogue_offer_items_1` FOREIGN KEY (`offer_id`) REFERENCES `catalogue_offers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_catalogue_offer_items_2` FOREIGN KEY (`definition_id`) REFERENCES `furni_definitions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What an offer holds: one row, or several for a bundle.';

CREATE TABLE `offer_preview_images` (
  `offer_name` varchar(70) NOT NULL,
  `image` varchar(100) NOT NULL,
  PRIMARY KEY (`offer_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The preview image an offer shows, by the offer''s name.';

CREATE TABLE `catalogue_featured_slots` (
  `slot` int(11) NOT NULL,
  `image` varchar(70) NOT NULL DEFAULT '',
  `caption` varchar(130) NOT NULL DEFAULT '',
  `link_kind` enum('page_name','page_id','product_name') NOT NULL DEFAULT 'page_name',
  `expires_at` int(10) unsigned DEFAULT NULL,
  `page_name` varchar(30) NOT NULL DEFAULT '',
  `page_id` int(11) DEFAULT NULL,
  `product_name` varchar(40) NOT NULL DEFAULT '',
  PRIMARY KEY (`slot`),
  KEY `fk_catalogue_featured_slots_1` (`page_id`),
  CONSTRAINT `fk_catalogue_featured_slots_1` FOREIGN KEY (`page_id`) REFERENCES `catalogue_pages` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The catalogue front page''s featured slots.';

CREATE TABLE `catalogue_limited_sales` (
  `offer_id` int(11) NOT NULL,
  `number` int(11) NOT NULL,
  `player_id` int(11) DEFAULT NULL COMMENT 'The buyer; NULL once that player is gone.',
  `sold_at` int(10) unsigned NOT NULL,
  `furni_id` int(11) NOT NULL,
  PRIMARY KEY (`offer_id`,`number`),
  KEY `ix_catalogue_limited_sales_1` (`player_id`,`sold_at`),
  CONSTRAINT `fk_catalogue_limited_sales_1` FOREIGN KEY (`offer_id`) REFERENCES `catalogue_offers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_catalogue_limited_sales_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The sold numbers of a limited offer: a number without a row is for sale.';

CREATE TABLE `club_offers` (
  `id` int(11) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `name` varchar(35) NOT NULL,
  `days` int(11) NOT NULL DEFAULT 0,
  `price_credits` int(11) NOT NULL DEFAULT 10,
  `price_currency` int(11) NOT NULL DEFAULT 0,
  `currency` int(11) NOT NULL DEFAULT 0,
  `kind` enum('club','vip','build_pass','build_pass_addon') NOT NULL DEFAULT 'club',
  `is_deal` tinyint(1) NOT NULL DEFAULT 0,
  `giftable` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Membership lengths on sale.';

CREATE TABLE `gift_wrappers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sprite_id` int(11) NOT NULL DEFAULT 0,
  `definition_id` int(11) NOT NULL,
  `kind` enum('gift','wrapper') NOT NULL DEFAULT 'wrapper',
  PRIMARY KEY (`id`),
  KEY `fk_gift_wrappers_1` (`definition_id`),
  CONSTRAINT `fk_gift_wrappers_1` FOREIGN KEY (`definition_id`) REFERENCES `furni_definitions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Gift box and wrapping choices.';

CREATE TABLE `purchases` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) NOT NULL,
  `offer_id` int(11) DEFAULT NULL,
  `offer_name` varchar(255) NOT NULL DEFAULT '',
  `price_credits` int(11) NOT NULL DEFAULT 0,
  `price_currency` int(11) NOT NULL DEFAULT 0,
  `currency` int(11) NOT NULL DEFAULT 0,
  `amount` int(11) NOT NULL DEFAULT 1,
  `bought_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_purchases_1` (`player_id`,`bought_at`),
  KEY `ix_purchases_2` (`bought_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Catalogue purchases; the mod tool reads the last one.';

CREATE TABLE `purchase_furni` (
  `purchase_id` int(11) NOT NULL,
  `furni_id` int(11) NOT NULL,
  PRIMARY KEY (`purchase_id`,`furni_id`),
  CONSTRAINT `fk_purchase_furni_1` FOREIGN KEY (`purchase_id`) REFERENCES `purchases` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The furni a purchase made.';

CREATE TABLE `vouchers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(32) NOT NULL,
  `credits` int(11) NOT NULL DEFAULT 0,
  `currency_amount` int(11) NOT NULL DEFAULT 0,
  `currency` int(11) NOT NULL DEFAULT 0,
  `offer_id` int(11) DEFAULT NULL,
  `uses_left` int(11) DEFAULT 1 COMMENT 'Counts down with every use; NULL is no limit.',
  `uses_per_player` int(11) DEFAULT 1 COMMENT 'NULL is no limit.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_vouchers_1` (`code`),
  KEY `fk_vouchers_1` (`offer_id`),
  CONSTRAINT `fk_vouchers_1` FOREIGN KEY (`offer_id`) REFERENCES `catalogue_offers` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Codes that give something.';

CREATE TABLE `voucher_uses` (
  `voucher_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `uses` int(11) NOT NULL DEFAULT 0,
  `last_used_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`voucher_id`,`player_id`),
  KEY `fk_voucher_uses_2` (`player_id`),
  CONSTRAINT `fk_voucher_uses_1` FOREIGN KEY (`voucher_id`) REFERENCES `vouchers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_voucher_uses_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='How often a player used a voucher.';

CREATE TABLE `claimable_products` (
  `claim_code` varchar(64) NOT NULL,
  `choice` int(11) NOT NULL DEFAULT 0,
  `badge_code` varchar(32) NOT NULL DEFAULT '',
  `furni_code` varchar(70) NOT NULL DEFAULT '',
  `ends_at` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`claim_code`,`choice`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Free things to claim, by claim code and choice.';

CREATE TABLE `player_claimed_products` (
  `player_id` int(11) NOT NULL,
  `claim_code` varchar(64) NOT NULL,
  `choice` int(11) NOT NULL DEFAULT 0,
  `claimed_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`player_id`,`claim_code`),
  CONSTRAINT `fk_player_claimed_products_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What a player has claimed.';

CREATE TABLE `welcome_gifts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `kind` enum('item','room') NOT NULL DEFAULT 'item',
  `value` varchar(32) NOT NULL,
  `image` varchar(256) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Choices a new player gets.';

CREATE TABLE `target_offers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(32) NOT NULL,
  `title` varchar(128) NOT NULL DEFAULT '',
  `description` varchar(2048) NOT NULL DEFAULT '',
  `image` varchar(128) NOT NULL,
  `icon` varchar(128) NOT NULL,
  `ends_at` int(10) unsigned NOT NULL,
  `price_credits` int(11) NOT NULL DEFAULT 10,
  `price_currency` int(11) NOT NULL DEFAULT 10,
  `currency` int(11) NOT NULL DEFAULT 5,
  `purchase_limit` int(11) NOT NULL DEFAULT 5,
  `offer_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_target_offers_1` (`code`),
  KEY `fk_target_offers_1` (`offer_id`),
  CONSTRAINT `fk_target_offers_1` FOREIGN KEY (`offer_id`) REFERENCES `catalogue_offers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A personal offer popup.';

CREATE TABLE `target_offer_vars` (
  `target_offer_id` int(11) NOT NULL,
  `position` tinyint(4) NOT NULL DEFAULT 0,
  `value` varchar(255) NOT NULL,
  PRIMARY KEY (`target_offer_id`,`position`),
  CONSTRAINT `fk_target_offer_vars_1` FOREIGN KEY (`target_offer_id`) REFERENCES `target_offers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Strings a target offer hands the client.';

CREATE TABLE `player_target_offers` (
  `player_id` int(11) NOT NULL,
  `target_offer_id` int(11) NOT NULL,
  `state` int(11) NOT NULL DEFAULT 0,
  `bought` int(11) NOT NULL DEFAULT 0,
  `last_bought_at` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`player_id`,`target_offer_id`),
  KEY `fk_player_target_offers_2` (`target_offer_id`),
  CONSTRAINT `fk_player_target_offers_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_player_target_offers_2` FOREIGN KEY (`target_offer_id`) REFERENCES `target_offers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A player''s state on a target offer.';

CREATE TABLE `player_subscriptions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) NOT NULL,
  `kind` enum('club','vip','build_pass','build_pass_addon') NOT NULL,
  `started_at` int(10) unsigned NOT NULL,
  `duration_seconds` int(10) unsigned NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `ix_player_subscriptions_1` (`player_id`,`kind`,`active`),
  CONSTRAINT `fk_player_subscriptions_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Memberships a player holds or held.';

CREATE TABLE `club_paydays` (
  `player_id` int(11) NOT NULL,
  `round_at` int(10) unsigned NOT NULL COMMENT 'The moment the pay day fell due; a member is paid once per round.',
  `paid_at` int(10) unsigned NOT NULL,
  `streak_days` int(11) NOT NULL DEFAULT 0,
  `spent` int(11) NOT NULL DEFAULT 0,
  `streak_reward` int(11) NOT NULL DEFAULT 0,
  `spending_reward` int(11) NOT NULL DEFAULT 0,
  `currency` int(11) NOT NULL DEFAULT -1 COMMENT '-1 is credits, otherwise an activity point type.',
  PRIMARY KEY (`player_id`,`round_at`),
  CONSTRAINT `fk_club_paydays_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What each club pay day paid a member.';

CREATE TABLE `club_payday_rounds` (
  `round_at` int(10) unsigned NOT NULL COMMENT 'The moment the pay day fell due.',
  `settled_at` int(10) unsigned NOT NULL COMMENT 'When its members had been paid.',
  PRIMARY KEY (`round_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Club pay days that were paid out. The round due next is the first pay day after the latest row.';

CREATE TABLE `marketplace_listings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `furni_id` int(11) NOT NULL,
  `player_id` int(11) DEFAULT NULL,
  `price` int(11) NOT NULL DEFAULT 0,
  `listed_at` int(10) unsigned NOT NULL,
  `sold_at` int(10) unsigned DEFAULT NULL,
  `state` tinyint(4) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `ix_marketplace_listings_1` (`player_id`,`id`),
  KEY `ix_marketplace_listings_2` (`state`,`listed_at`,`price`),
  KEY `ix_marketplace_listings_3` (`state`,`sold_at`),
  KEY `ix_marketplace_listings_4` (`furni_id`),
  CONSTRAINT `fk_marketplace_listings_1` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_marketplace_listings_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Furni on the marketplace. While listed, the furni''s owner is NULL.';

CREATE TABLE `trades` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `first_player_id` int(11) NOT NULL,
  `second_player_id` int(11) NOT NULL,
  `first_ip` varchar(45) NOT NULL,
  `second_ip` varchar(45) NOT NULL,
  `traded_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_trades_1` (`first_player_id`),
  KEY `ix_trades_2` (`second_player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A finished trade between two players.';

CREATE TABLE `trade_items` (
  `trade_id` int(11) NOT NULL,
  `furni_id` int(11) NOT NULL,
  `receiver_id` int(11) NOT NULL,
  `redeemed_credits` int(11) NOT NULL DEFAULT 0 COMMENT 'Credit furni paid into the receiver''s purse: its value. The furni row is gone then.',
  PRIMARY KEY (`trade_id`,`furni_id`),
  KEY `ix_trade_items_1` (`receiver_id`),
  CONSTRAINT `fk_trade_items_1` FOREIGN KEY (`trade_id`) REFERENCES `trades` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The furni of a trade and who received it.';


-- Achievements, quests and rewards

CREATE TABLE `achievements` (
  `id` int(11) NOT NULL,
  `code` varchar(64) NOT NULL,
  `category` enum('identity','explore','music','social','games','room_builder','pets','tools','events','other','test','invisible','misc','archive','wired_games','crackables','crafting') NOT NULL DEFAULT 'identity',
  `subcategory` varchar(64) NOT NULL DEFAULT '',
  `state` smallint(6) NOT NULL DEFAULT 1,
  `display_method` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_achievements_1` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='An achievement.';

CREATE TABLE `achievement_levels` (
  `achievement_id` int(11) NOT NULL,
  `level` int(11) NOT NULL DEFAULT 1,
  `notification_id` int(11) NOT NULL DEFAULT 0,
  `progress_needed` int(11) NOT NULL DEFAULT 1,
  `score_points` int(11) NOT NULL DEFAULT 10,
  `reward_currency` int(11) NOT NULL DEFAULT 0,
  `reward_amount` int(11) NOT NULL DEFAULT 100,
  `bonus_points` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`achievement_id`,`level`),
  CONSTRAINT `fk_achievement_levels_1` FOREIGN KEY (`achievement_id`) REFERENCES `achievements` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A level of an achievement.';

CREATE TABLE `achievement_triggers` (
  `achievement_id` int(11) NOT NULL,
  `event` varchar(64) NOT NULL,
  `detail` varchar(64) NOT NULL DEFAULT '',
  PRIMARY KEY (`achievement_id`,`event`,`detail`),
  KEY `ix_achievement_triggers_1` (`event`),
  CONSTRAINT `fk_achievement_triggers_1` FOREIGN KEY (`achievement_id`) REFERENCES `achievements` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What the game counts towards an achievement: an event of the game, and for some events the one detail (a pet type, a room type, a furni name) it counts.';

CREATE TABLE `player_achievements` (
  `player_id` int(11) NOT NULL,
  `achievement_id` int(11) NOT NULL,
  `progress` int(11) NOT NULL DEFAULT 0,
  `rewarded_level` smallint(6) NOT NULL DEFAULT 0,
  PRIMARY KEY (`player_id`,`achievement_id`),
  KEY `ix_player_achievements_1` (`achievement_id`),
  CONSTRAINT `fk_player_achievements_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_player_achievements_2` FOREIGN KEY (`achievement_id`) REFERENCES `achievements` (`id`) ON DELETE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Everything counted for a player on an achievement and the highest level handed out for it.';

CREATE TABLE `achievement_resolutions` (
  `furni_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `achievement_id` int(11) NOT NULL,
  `target_level` smallint(5) unsigned NOT NULL,
  `ends_at` int(10) unsigned NOT NULL,
  `completed_at` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`furni_id`),
  KEY `ix_achievement_resolutions_1` (`player_id`,`achievement_id`),
  CONSTRAINT `fk_achievement_resolutions_1` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_achievement_resolutions_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A resolution furni''s promise: the level of an achievement its player means to reach before ends_at. The badge shown is the one the achievement names for that level.';

CREATE TABLE `quests` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `campaign_code` varchar(64) NOT NULL,
  `code` varchar(64) NOT NULL,
  `chain_code` varchar(64) NOT NULL DEFAULT '',
  `sort_order` int(11) NOT NULL DEFAULT 0,
  `goal_type` varchar(32) NOT NULL,
  `goal_data` varchar(255) NOT NULL DEFAULT '',
  `goal_count` int(11) NOT NULL DEFAULT 1,
  `reward_currency` int(11) NOT NULL DEFAULT 0,
  `reward_amount` int(11) NOT NULL DEFAULT 0,
  `reward_badge` varchar(32) NOT NULL DEFAULT '',
  `is_easy` tinyint(1) NOT NULL DEFAULT 1,
  `is_daily` tinyint(1) NOT NULL DEFAULT 0,
  `seasonal_ends_at` int(10) unsigned NOT NULL DEFAULT 0 COMMENT 'Unix time the seasonal campaign ends; 0 = not seasonal.',
  `wait_seconds` int(11) NOT NULL DEFAULT 0,
  `catalogue_page_name` varchar(64) NOT NULL DEFAULT '',
  `image_version` varchar(16) NOT NULL DEFAULT '',
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_quests_1` (`campaign_code`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A quest in a campaign.';

CREATE TABLE `player_quests` (
  `player_id` int(11) NOT NULL,
  `quest_id` int(11) NOT NULL,
  `progress` int(11) NOT NULL DEFAULT 0,
  `tracked_since` int(10) unsigned DEFAULT NULL COMMENT 'When the player took the quest up; NULL while it is not followed.',
  `completed_at` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`player_id`,`quest_id`),
  KEY `fk_player_quests_2` (`quest_id`),
  CONSTRAINT `fk_player_quests_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_player_quests_2` FOREIGN KEY (`quest_id`) REFERENCES `quests` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A player''s progress on a quest.';

CREATE TABLE `daily_tasks` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  `goal_type` varchar(32) NOT NULL,
  `goal_data` varchar(255) NOT NULL DEFAULT '',
  `required_repeats` int(11) NOT NULL DEFAULT 1,
  `is_bonus` tinyint(1) NOT NULL DEFAULT 0,
  `reward_type` varchar(32) NOT NULL DEFAULT '',
  `reward_extra` varchar(255) NOT NULL DEFAULT '',
  `reward_amount` int(11) NOT NULL DEFAULT 0,
  `catalogue_name` varchar(64) NOT NULL DEFAULT '',
  `image_version` varchar(16) NOT NULL DEFAULT '',
  `sort_order` int(11) NOT NULL DEFAULT 0,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_daily_tasks_1` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A task in the daily list.';

CREATE TABLE `player_daily_tasks` (
  `player_id` int(11) NOT NULL,
  `task_id` int(11) NOT NULL,
  `day` date NOT NULL,
  `repeats` int(11) NOT NULL DEFAULT 0,
  `status` tinyint(4) NOT NULL DEFAULT 0,
  `updated_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`player_id`,`task_id`,`day`),
  KEY `fk_player_daily_tasks_2` (`task_id`),
  CONSTRAINT `fk_player_daily_tasks_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_player_daily_tasks_2` FOREIGN KEY (`task_id`) REFERENCES `daily_tasks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A player''s progress on a task on a day.';

CREATE TABLE `player_daily_task_days` (
  `player_id` int(11) NOT NULL,
  `day` date NOT NULL,
  PRIMARY KEY (`player_id`,`day`),
  CONSTRAINT `fk_player_daily_task_days_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Days a player finished all daily tasks.';

CREATE TABLE `player_daily_gifts` (
  `player_id` int(11) NOT NULL,
  `last_day` int(11) NOT NULL DEFAULT 0,
  `last_status_day` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`player_id`),
  CONSTRAINT `fk_player_daily_gifts_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='When a player last took the daily gift.';

CREATE TABLE `reward_tracks` (
  `id` varchar(64) NOT NULL,
  `theme` varchar(32) NOT NULL,
  `sort_order` int(11) NOT NULL DEFAULT 0,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A reward track (introduction, citizenship..).';

CREATE TABLE `reward_track_tasks` (
  `track_id` varchar(64) NOT NULL,
  `id` varchar(64) NOT NULL,
  `action_type` varchar(64) NOT NULL,
  `parameter` varchar(255) NOT NULL DEFAULT '',
  `is_premium` tinyint(1) NOT NULL DEFAULT 0,
  `sort_order` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`track_id`,`id`),
  CONSTRAINT `fk_reward_track_tasks_1` FOREIGN KEY (`track_id`) REFERENCES `reward_tracks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Something a track counts.';

CREATE TABLE `reward_track_levels` (
  `track_id` varchar(64) NOT NULL,
  `task_id` varchar(64) NOT NULL,
  `level` int(11) NOT NULL DEFAULT 0,
  `required_count` int(11) NOT NULL DEFAULT 0,
  `points` int(11) NOT NULL DEFAULT 0,
  `is_premium` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`track_id`,`task_id`,`level`),
  CONSTRAINT `fk_reward_track_levels_1` FOREIGN KEY (`track_id`, `task_id`) REFERENCES `reward_track_tasks` (`track_id`, `id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Levels of a track''s task and their points.';

CREATE TABLE `reward_track_prizes` (
  `track_id` varchar(64) NOT NULL,
  `id` varchar(64) NOT NULL,
  `sort_order` int(11) NOT NULL DEFAULT 0,
  `required_points` int(11) NOT NULL DEFAULT 0,
  `product_type` smallint(6) NOT NULL,
  `product_code` varchar(255) NOT NULL DEFAULT '',
  `extra` varchar(512) NOT NULL DEFAULT '',
  `amount` int(11) NOT NULL DEFAULT 1,
  `is_premium` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`track_id`,`id`),
  CONSTRAINT `fk_reward_track_prizes_1` FOREIGN KEY (`track_id`) REFERENCES `reward_tracks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What a track gives at a number of points.';

CREATE TABLE `player_reward_track_points` (
  `player_id` int(11) NOT NULL,
  `track_id` varchar(64) NOT NULL,
  `points` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`player_id`,`track_id`),
  KEY `fk_player_reward_track_points_2` (`track_id`),
  CONSTRAINT `fk_player_reward_track_points_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_player_reward_track_points_2` FOREIGN KEY (`track_id`) REFERENCES `reward_tracks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A player''s points on a track.';

CREATE TABLE `player_reward_track_progress` (
  `player_id` int(11) NOT NULL,
  `track_id` varchar(64) NOT NULL,
  `task_id` varchar(64) NOT NULL,
  `progress` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`player_id`,`track_id`,`task_id`),
  KEY `fk_player_reward_track_progress_2` (`track_id`,`task_id`),
  CONSTRAINT `fk_player_reward_track_progress_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_player_reward_track_progress_2` FOREIGN KEY (`track_id`, `task_id`) REFERENCES `reward_track_tasks` (`track_id`, `id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A player''s count on a track''s task.';

CREATE TABLE `player_reward_track_claims` (
  `player_id` int(11) NOT NULL,
  `track_id` varchar(64) NOT NULL,
  `prize_id` varchar(64) NOT NULL,
  `claimed_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`player_id`,`track_id`,`prize_id`),
  KEY `fk_player_reward_track_claims_2` (`track_id`,`prize_id`),
  CONSTRAINT `fk_player_reward_track_claims_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_player_reward_track_claims_2` FOREIGN KEY (`track_id`, `prize_id`) REFERENCES `reward_track_prizes` (`track_id`, `id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prizes a player has taken.';

CREATE TABLE `player_reward_track_passes` (
  `player_id` int(11) NOT NULL,
  `track_id` varchar(64) NOT NULL,
  `bought_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`player_id`,`track_id`),
  KEY `fk_player_reward_track_passes_2` (`track_id`),
  CONSTRAINT `fk_player_reward_track_passes_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_player_reward_track_passes_2` FOREIGN KEY (`track_id`) REFERENCES `reward_tracks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The premium passes a player bought, one per track.';

CREATE TABLE `player_income_rewards` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) NOT NULL,
  `category` tinyint(4) NOT NULL DEFAULT 0,
  `reward_type` tinyint(4) NOT NULL DEFAULT 0,
  `amount` int(11) NOT NULL DEFAULT 0,
  `product_code` varchar(100) NOT NULL DEFAULT '',
  `created_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_player_income_rewards_1` (`player_id`),
  CONSTRAINT `fk_player_income_rewards_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Rewards waiting in the earnings center.';

CREATE TABLE `calendar_campaigns` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT 'What the client and the setting calendar.campaign call it.',
  `image` varchar(255) NOT NULL DEFAULT '',
  `first_day` date NOT NULL COMMENT 'The day of door 0, in game time.',
  `last_day` date NOT NULL COMMENT 'The day of the last door.',
  `catch_up_days` int(11) DEFAULT 2 COMMENT 'Days a missed door stays open; NULL keeps every passed door open.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_calendar_campaigns_1` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Campaign calendars. One runs on the days from its first to its last.';

CREATE TABLE `calendar_prizes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `campaign_id` int(11) NOT NULL,
  `day` int(11) DEFAULT NULL COMMENT 'The door (from 0) this prize is behind; NULL is drawn for any door without its own.',
  `door_text` varchar(128) NOT NULL DEFAULT '' COMMENT 'Shown when the door opens; %amount%, %badge%, %days% and %item% are filled in.',
  `image` varchar(128) NOT NULL DEFAULT '',
  `currency` int(11) DEFAULT NULL COMMENT '-1 is credits, otherwise an activity point type; NULL pays nothing.',
  `amount` int(11) NOT NULL DEFAULT 0,
  `badge_code` varchar(25) DEFAULT NULL,
  `definition_id` int(11) DEFAULT NULL,
  `membership` varchar(32) DEFAULT NULL COMMENT 'Kind of membership the prize adds days to.',
  `membership_days` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `ix_calendar_prizes_1` (`campaign_id`,`day`),
  KEY `fk_calendar_prizes_2` (`definition_id`),
  CONSTRAINT `fk_calendar_prizes_1` FOREIGN KEY (`campaign_id`) REFERENCES `calendar_campaigns` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_calendar_prizes_2` FOREIGN KEY (`definition_id`) REFERENCES `furni_definitions` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What a calendar door gives.';

CREATE TABLE `calendar_opened_doors` (
  `campaign_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `day` int(11) NOT NULL,
  `prize_id` int(11) NOT NULL COMMENT 'What was behind the door; kept when the prize row is gone.',
  `opened_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`campaign_id`,`player_id`,`day`),
  KEY `fk_calendar_opened_doors_2` (`player_id`),
  CONSTRAINT `fk_calendar_opened_doors_1` FOREIGN KEY (`campaign_id`) REFERENCES `calendar_campaigns` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_calendar_opened_doors_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Doors a player opened.';

CREATE TABLE `community_goals` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  `opens_at` int(10) unsigned NOT NULL DEFAULT 0,
  `closes_at` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_community_goals_1` (`code`),
  KEY `ix_community_goals_1` (`opens_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A community goal. The landing view shows the goal that opened last; one that opens later waits its turn, and the community total is the sum of community_goal_scores.';

CREATE TABLE `community_goal_levels` (
  `goal_id` int(11) NOT NULL,
  `level` tinyint(4) NOT NULL DEFAULT 0,
  `threshold` int(11) NOT NULL DEFAULT 0,
  `rewarded_players` int(11) DEFAULT NULL,
  PRIMARY KEY (`goal_id`,`level`),
  CONSTRAINT `fk_community_goal_levels_1` FOREIGN KEY (`goal_id`) REFERENCES `community_goals` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Thresholds and how many players a level rewards.';

CREATE TABLE `community_goal_options` (
  `goal_id` int(11) NOT NULL,
  `option_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`goal_id`,`option_id`),
  CONSTRAINT `fk_community_goal_options_1` FOREIGN KEY (`goal_id`) REFERENCES `community_goals` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What players can vote on in a goal.';

CREATE TABLE `community_goal_scores` (
  `goal_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `score` int(10) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`goal_id`,`player_id`),
  KEY `ix_community_goal_scores_1` (`goal_id`,`score`),
  KEY `fk_community_goal_scores_2` (`player_id`),
  CONSTRAINT `fk_community_goal_scores_1` FOREIGN KEY (`goal_id`) REFERENCES `community_goals` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_community_goal_scores_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What a player added to a goal. The index answers both the total and a player''s rank.';

CREATE TABLE `community_goal_votes` (
  `goal_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `option_id` int(11) NOT NULL,
  PRIMARY KEY (`goal_id`,`player_id`),
  KEY `fk_community_goal_votes_1` (`goal_id`,`option_id`),
  KEY `fk_community_goal_votes_2` (`player_id`),
  CONSTRAINT `fk_community_goal_votes_1` FOREIGN KEY (`goal_id`,`option_id`) REFERENCES `community_goal_options` (`goal_id`,`option_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_community_goal_votes_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A player''s vote for one of the goal''s options. The key is what allows one vote per player: the writer inserts from community_goal_options with INSERT IGNORE and reads the row count.';

CREATE TABLE `community_goal_claims` (
  `goal_code` varchar(64) NOT NULL,
  `player_id` int(11) NOT NULL,
  `claimed_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`goal_code`,`player_id`),
  KEY `fk_community_goal_claims_1` (`player_id`),
  CONSTRAINT `fk_community_goal_claims_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Who took a goal''s reward.';

CREATE TABLE `treasure_hunts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  `is_open` tinyint(1) NOT NULL DEFAULT 1,
  `level_needed` smallint(5) unsigned NOT NULL DEFAULT 0,
  `club_level_needed` smallint(5) unsigned NOT NULL DEFAULT 0,
  `first_finisher_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_treasure_hunts_1` (`code`),
  KEY `fk_treasure_hunts_1` (`first_finisher_id`),
  CONSTRAINT `fk_treasure_hunts_1` FOREIGN KEY (`first_finisher_id`) REFERENCES `players` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A treasure hunt, the account level it asks of players without and with club, and who finished it first. The first place is taken by an UPDATE that only matches while the column is NULL.';

CREATE TABLE `treasure_hunt_clues` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `hunt_id` int(11) NOT NULL,
  `furni_id` int(11) DEFAULT NULL,
  `step_code` varchar(64) DEFAULT NULL,
  `is_open` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_treasure_hunt_clues_1` (`furni_id`),
  UNIQUE KEY `uq_treasure_hunt_clues_2` (`hunt_id`,`step_code`),
  CONSTRAINT `fk_treasure_hunt_clues_1` FOREIGN KEY (`hunt_id`) REFERENCES `treasure_hunts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_treasure_hunt_clues_2` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ck_treasure_hunt_clues_place` CHECK ((`furni_id` IS NULL) <> (`step_code` IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='One thing to find in a hunt: a furni to click or a step code the client reports. A hunt is finished with all its open clues.';

CREATE TABLE `treasure_hunt_prizes` (
  `hunt_id` int(11) NOT NULL,
  `position` tinyint(3) unsigned NOT NULL,
  `badge_code` varchar(64) DEFAULT NULL,
  `definition_id` int(11) DEFAULT NULL,
  `points_type` int(11) DEFAULT NULL,
  `amount` int(10) unsigned NOT NULL DEFAULT 1,
  PRIMARY KEY (`hunt_id`,`position`),
  KEY `fk_treasure_hunt_prizes_2` (`definition_id`),
  CONSTRAINT `fk_treasure_hunt_prizes_1` FOREIGN KEY (`hunt_id`) REFERENCES `treasure_hunts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_treasure_hunt_prizes_2` FOREIGN KEY (`definition_id`) REFERENCES `furni_definitions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What finishing a hunt pays: per row a badge, amount copies of a furni or amount points of a type.';

CREATE TABLE `treasure_hunt_found` (
  `player_id` int(11) NOT NULL,
  `clue_id` int(11) NOT NULL,
  `found_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`player_id`,`clue_id`),
  KEY `fk_treasure_hunt_found_2` (`clue_id`),
  CONSTRAINT `fk_treasure_hunt_found_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_treasure_hunt_found_2` FOREIGN KEY (`clue_id`) REFERENCES `treasure_hunt_clues` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A clue a player found. How far a player is in a hunt is the number of these rows on the hunt''s open clues.';

CREATE TABLE `treasure_hunt_finishers` (
  `hunt_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `finished_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`hunt_id`,`player_id`),
  KEY `fk_treasure_hunt_finishers_2` (`player_id`),
  CONSTRAINT `fk_treasure_hunt_finishers_1` FOREIGN KEY (`hunt_id`) REFERENCES `treasure_hunts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_treasure_hunt_finishers_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A player who finished a hunt. The key is what pays the prizes once: the writer is INSERT IGNORE and reads the row count.';


-- Pets and bots

CREATE TABLE `pet_types` (
  `id` int(11) NOT NULL,
  `name` varchar(32) NOT NULL,
  `offspring_type_id` int(11) DEFAULT NULL,
  `can_swim` tinyint(1) NOT NULL DEFAULT 0,
  `behaviour` enum('plain','gnome','leprechaun','cow') NOT NULL DEFAULT 'plain',
  `product_name` varchar(70) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A pet type: its name, offspring, whether it swims, what it does beyond a plain pet and the product it is sold as.';

CREATE TABLE `pet_boxes` (
  `furni_name` varchar(70) NOT NULL,
  `type_id` int(11) NOT NULL,
  `palette` int(11) DEFAULT NULL,
  PRIMARY KEY (`furni_name`),
  KEY `fk_pet_boxes_1` (`type_id`),
  CONSTRAINT `fk_pet_boxes_1` FOREIGN KEY (`type_id`) REFERENCES `pet_types` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A furni that hatches a pet; with a palette it always hatches that one.';

CREATE TABLE `pet_type_actions` (
  `type_id` int(11) NOT NULL,
  `mood` enum('happy','tired','random') NOT NULL,
  `action` varchar(32) NOT NULL,
  PRIMARY KEY (`type_id`,`mood`,`action`),
  CONSTRAINT `fk_pet_type_actions_1` FOREIGN KEY (`type_id`) REFERENCES `pet_types` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What a pet type does in each mood.';

CREATE TABLE `pet_commands` (
  `id` int(11) NOT NULL,
  `word` varchar(15) NOT NULL,
  `level` int(11) NOT NULL DEFAULT 1,
  `experience` int(11) NOT NULL DEFAULT 0 COMMENT 'Gained when the pet follows it, and what it costs in energy.',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Commands a pet can learn.';

CREATE TABLE `pet_type_commands` (
  `type_id` int(11) NOT NULL,
  `command_id` int(11) NOT NULL,
  `required_level` int(11) DEFAULT NULL,
  `sort_order` int(11) DEFAULT NULL,
  PRIMARY KEY (`type_id`,`command_id`),
  KEY `fk_pet_type_commands_2` (`command_id`),
  CONSTRAINT `fk_pet_type_commands_1` FOREIGN KEY (`type_id`) REFERENCES `pet_types` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pet_type_commands_2` FOREIGN KEY (`command_id`) REFERENCES `pet_commands` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Which commands a pet type has and from which level.';

CREATE TABLE `pet_vocals` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type_id` int(11) DEFAULT NULL,
  `situation` enum('DISOBEY','DRINKING','EATING','GENERIC_HAPPY','GENERIC_NEUTRAL','GENERIC_SAD','GREET_OWNER','HUNGRY','LEVEL_UP','MUTED','PLAYFUL','PLAYING','REVIVED','SLEEPING','THIRSTY','TIRED','TREATED','UNKNOWN_COMMAND') NOT NULL DEFAULT 'GENERIC_HAPPY',
  `message` varchar(255) NOT NULL,
  `text_key` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `ix_pet_vocals_1` (`type_id`,`situation`),
  CONSTRAINT `fk_pet_vocals_1` FOREIGN KEY (`type_id`) REFERENCES `pet_types` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What a pet type says in a situation.';

CREATE TABLE `pet_palettes` (
  `type_id` int(11) NOT NULL,
  `breed` int(11) NOT NULL,
  `palette` int(11) NOT NULL,
  `sellable` tinyint(1) NOT NULL DEFAULT 0,
  `rare` tinyint(1) NOT NULL DEFAULT 0,
  `club_only` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`type_id`,`breed`,`palette`),
  CONSTRAINT `fk_pet_palettes_1` FOREIGN KEY (`type_id`) REFERENCES `pet_types` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The breed and palette pairs a pet type comes in, as its buy page lists them.';

CREATE TABLE `pet_baby_breeds` (
  `type_id` int(11) NOT NULL,
  `breed` int(11) NOT NULL,
  `tier` enum('epic','rare','uncommon','common') NOT NULL,
  PRIMARY KEY (`type_id`,`breed`),
  CONSTRAINT `fk_pet_baby_breeds_1` FOREIGN KEY (`type_id`) REFERENCES `pet_types` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The breeds a baby pet type is born in and how rare each one is.';

CREATE TABLE `pet_furni` (
  `type_id` int(11) DEFAULT NULL,
  `definition_id` int(11) NOT NULL,
  UNIQUE KEY `uq_pet_furni_1` (`definition_id`,`type_id`),
  KEY `fk_pet_furni_1` (`type_id`),
  CONSTRAINT `fk_pet_furni_1` FOREIGN KEY (`type_id`) REFERENCES `pet_types` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pet_furni_2` FOREIGN KEY (`definition_id`) REFERENCES `furni_definitions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Furni a pet type uses (nest, food, drink).';

CREATE TABLE `pet_packages` (
  `furni_code` varchar(70) NOT NULL,
  `type_id` int(11) NOT NULL,
  `palette` int(11) NOT NULL DEFAULT 0,
  `color` char(6) NOT NULL DEFAULT 'ffffff',
  `nest_furni_code` varchar(70) NOT NULL,
  PRIMARY KEY (`furni_code`),
  KEY `fk_pet_packages_1` (`type_id`),
  CONSTRAINT `fk_pet_packages_1` FOREIGN KEY (`type_id`) REFERENCES `pet_types` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Pet bought as a package furni.';

CREATE TABLE `pets` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) NOT NULL,
  `room_id` int(11) DEFAULT NULL,
  `name` varchar(32) NOT NULL,
  `type_id` int(11) NOT NULL,
  `race` int(11) NOT NULL DEFAULT 0,
  `color` char(6) NOT NULL,
  `happiness` int(11) NOT NULL DEFAULT 100,
  `experience` int(11) NOT NULL DEFAULT 0,
  `energy` int(11) NOT NULL DEFAULT 100,
  `thirst` int(11) NOT NULL DEFAULT 0,
  `respect` int(11) NOT NULL DEFAULT 0,
  `born_at` int(10) unsigned NOT NULL,
  `x` smallint(6) NOT NULL DEFAULT 0,
  `y` smallint(6) NOT NULL DEFAULT 0,
  `z` double NOT NULL DEFAULT 0,
  `direction` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `ix_pets_1` (`player_id`,`room_id`),
  KEY `ix_pets_2` (`room_id`,`id`),
  KEY `fk_pets_3` (`type_id`),
  CONSTRAINT `fk_pets_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pets_2` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_pets_3` FOREIGN KEY (`type_id`) REFERENCES `pet_types` (`id`) ON DELETE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A player''s pet.';

CREATE TABLE `monster_plants` (
  `pet_id` int(11) NOT NULL,
  `kind` int(11) NOT NULL DEFAULT 0,
  `color` int(11) NOT NULL DEFAULT 0,
  `nose` int(11) NOT NULL DEFAULT 0,
  `nose_color` tinyint(4) NOT NULL DEFAULT 0,
  `eyes` int(11) NOT NULL DEFAULT 0,
  `eyes_color` tinyint(4) NOT NULL DEFAULT 0,
  `mouth` int(11) NOT NULL DEFAULT 0,
  `mouth_color` tinyint(4) NOT NULL DEFAULT 0,
  `base_palette` tinyint(4) NOT NULL DEFAULT 10,
  `dies_at` int(10) unsigned DEFAULT NULL,
  `is_dead` tinyint(1) NOT NULL DEFAULT 0,
  `shown_stage` tinyint(4) DEFAULT NULL COMMENT 'The stage it last showed in a loaded room; NULL until it showed one.',
  `breedable` tinyint(1) NOT NULL DEFAULT 0,
  `allow_breeding` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`pet_id`),
  CONSTRAINT `fk_monster_plants_1` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What only a monster plant has.';

CREATE TABLE `horses` (
  `pet_id` int(11) NOT NULL,
  `mane_style` int(11) NOT NULL DEFAULT -1,
  `mane_tint` int(11) NOT NULL DEFAULT 1,
  `saddle_definition_id` int(11) DEFAULT NULL COMMENT 'The saddle furni it wears; NULL without a saddle.',
  `open_to_riders` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`pet_id`),
  CONSTRAINT `fk_horses_1` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What only a horse has: its mane, its saddle and who may ride it.';

CREATE TABLE `pet_looks` (
  `pet_id` int(11) NOT NULL,
  `parts` varchar(80) NOT NULL,
  PRIMARY KEY (`pet_id`),
  CONSTRAINT `fk_pet_looks_1` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The figure parts of a pet that has its own (gnome, leprechaun, cow).';

CREATE TABLE `bots` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) NOT NULL,
  `room_id` int(11) DEFAULT NULL,
  `name` varchar(25) NOT NULL DEFAULT '',
  `motto` varchar(100) NOT NULL DEFAULT '',
  `figure` varchar(500) NOT NULL DEFAULT '',
  `gender` enum('M','F') NOT NULL DEFAULT 'M',
  `x` smallint(6) NOT NULL DEFAULT 0,
  `y` smallint(6) NOT NULL DEFAULT 0,
  `z` double NOT NULL DEFAULT 0,
  `direction` tinyint(4) NOT NULL DEFAULT 0,
  `chat_auto` tinyint(1) NOT NULL DEFAULT 1,
  `chat_random` tinyint(1) NOT NULL DEFAULT 1,
  `chat_delay_seconds` int(11) NOT NULL DEFAULT 5,
  `dance` tinyint(4) NOT NULL DEFAULT 0,
  `roams` tinyint(1) NOT NULL DEFAULT 0,
  `kind` varchar(32) NOT NULL DEFAULT 'generic',
  `effect_id` int(11) NOT NULL DEFAULT 0,
  `bubble_id` int(11) NOT NULL DEFAULT 31,
  PRIMARY KEY (`id`),
  KEY `ix_bots_1` (`player_id`,`room_id`),
  KEY `ix_bots_2` (`room_id`),
  CONSTRAINT `fk_bots_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_bots_2` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A player''s bot.';

CREATE TABLE `bot_chat_lines` (
  `bot_id` int(11) NOT NULL,
  `position` smallint(6) NOT NULL,
  `line` varchar(255) NOT NULL,
  PRIMARY KEY (`bot_id`,`position`),
  CONSTRAINT `fk_bot_chat_lines_1` FOREIGN KEY (`bot_id`) REFERENCES `bots` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What a bot says.';

CREATE TABLE `bot_serve_keywords` (
  `keyword` varchar(128) NOT NULL,
  `handitem_id` int(11) NOT NULL,
  PRIMARY KEY (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Words a butler bot answers with a hand item.';


-- Guilds

CREATE TABLE `guilds` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `owner_id` int(11) NOT NULL,
  `name` varchar(50) NOT NULL DEFAULT '',
  `description` varchar(250) NOT NULL DEFAULT '',
  `room_id` int(11) DEFAULT NULL,
  `join_mode` tinyint(4) NOT NULL DEFAULT 0,
  `members_may_decorate` tinyint(1) NOT NULL DEFAULT 0,
  `color_one` int(11) NOT NULL DEFAULT 0,
  `color_two` int(11) NOT NULL DEFAULT 0,
  `badge_code` varchar(256) NOT NULL DEFAULT '',
  `created_at` int(10) unsigned NOT NULL,
  `has_forum` tinyint(1) NOT NULL DEFAULT 0,
  `forum_read` enum('EVERYONE','MEMBERS','ADMINS') NOT NULL DEFAULT 'EVERYONE',
  `forum_post` enum('EVERYONE','MEMBERS','ADMINS','OWNER') NOT NULL DEFAULT 'EVERYONE',
  `forum_new_thread` enum('EVERYONE','MEMBERS','ADMINS','OWNER') NOT NULL DEFAULT 'EVERYONE',
  `forum_moderate` enum('ADMINS','OWNER') NOT NULL DEFAULT 'ADMINS',
  PRIMARY KEY (`id`),
  KEY `ix_guilds_1` (`owner_id`),
  KEY `ix_guilds_2` (`room_id`),
  KEY `ix_guilds_3` (`name`),
  CONSTRAINT `fk_guilds_1` FOREIGN KEY (`owner_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_guilds_2` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A guild and its forum settings.';

CREATE TABLE `guild_members` (
  `guild_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `level` tinyint(4) NOT NULL DEFAULT 0,
  `member_since` int(10) unsigned NOT NULL,
  PRIMARY KEY (`guild_id`,`player_id`),
  KEY `ix_guild_members_1` (`player_id`,`guild_id`),
  KEY `ix_guild_members_2` (`guild_id`,`level`),
  CONSTRAINT `fk_guild_members_1` FOREIGN KEY (`guild_id`) REFERENCES `guilds` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_guild_members_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Who is in a guild and how.';

CREATE TABLE `guild_badge_parts` (
  `id` int(11) NOT NULL,
  `kind` enum('base','symbol','color','color2','color3') NOT NULL,
  `asset` varchar(300) NOT NULL,
  `asset_mask` varchar(300) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`,`kind`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Parts and colours of the badge editor.';

CREATE TABLE `guild_forum_threads` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `guild_id` int(11) NOT NULL,
  `author_id` int(11) NOT NULL,
  `subject` varchar(255) NOT NULL DEFAULT '',
  `post_count` int(11) NOT NULL DEFAULT 0,
  `last_post_id` int(11) DEFAULT NULL,
  `created_at` int(10) unsigned NOT NULL,
  `updated_at` int(10) unsigned NOT NULL,
  `state` tinyint(4) NOT NULL DEFAULT 0,
  `pinned` tinyint(1) NOT NULL DEFAULT 0,
  `locked` tinyint(1) NOT NULL DEFAULT 0,
  `moderator_id` int(11) DEFAULT NULL,
  `moderated_at` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_guild_forum_threads_1` (`guild_id`,`id`),
  KEY `fk_guild_forum_threads_2` (`author_id`),
  KEY `fk_guild_forum_threads_3` (`moderator_id`),
  CONSTRAINT `fk_guild_forum_threads_1` FOREIGN KEY (`guild_id`) REFERENCES `guilds` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_guild_forum_threads_2` FOREIGN KEY (`author_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_guild_forum_threads_3` FOREIGN KEY (`moderator_id`) REFERENCES `players` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Threads of a guild''s forum; each knows its newest post and how many it has.';

CREATE TABLE `guild_forum_posts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `thread_id` int(11) NOT NULL,
  `author_id` int(11) NOT NULL,
  `message` text NOT NULL,
  `created_at` int(10) unsigned NOT NULL,
  `state` tinyint(4) NOT NULL DEFAULT 0,
  `moderator_id` int(11) DEFAULT NULL,
  `moderated_at` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_guild_forum_posts_1` (`thread_id`,`id`),
  KEY `fk_guild_forum_posts_2` (`author_id`),
  KEY `fk_guild_forum_posts_3` (`moderator_id`),
  CONSTRAINT `fk_guild_forum_posts_1` FOREIGN KEY (`thread_id`) REFERENCES `guild_forum_threads` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_guild_forum_posts_2` FOREIGN KEY (`author_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_guild_forum_posts_3` FOREIGN KEY (`moderator_id`) REFERENCES `players` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Posts of a forum thread.';

CREATE TABLE `guild_forum_reads` (
  `player_id` int(11) NOT NULL,
  `guild_id` int(11) NOT NULL,
  `read_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`player_id`,`guild_id`),
  KEY `fk_guild_forum_reads_2` (`guild_id`),
  CONSTRAINT `fk_guild_forum_reads_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_guild_forum_reads_2` FOREIGN KEY (`guild_id`) REFERENCES `guilds` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='When a player last read a guild''s forum.';

CREATE TABLE `guild_forum_visits` (
  `player_id` int(11) NOT NULL,
  `guild_id` int(11) NOT NULL,
  `visited_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`player_id`,`guild_id`),
  KEY `ix_guild_forum_visits_1` (`visited_at`),
  KEY `fk_guild_forum_visits_2` (`guild_id`),
  CONSTRAINT `fk_guild_forum_visits_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_guild_forum_visits_2` FOREIGN KEY (`guild_id`) REFERENCES `guilds` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='When a player last opened a guild''s forum, for the most viewed list.';


-- Games, competitions and polls

CREATE TABLE `room_game_results` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `room_id` int(11) NOT NULL,
  `game` varchar(32) NOT NULL,
  `started_at` int(10) unsigned NOT NULL,
  `ended_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_room_game_results_1` (`room_id`,`ended_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A finished room game.';

CREATE TABLE `room_game_result_teams` (
  `result_id` int(11) NOT NULL,
  `team` tinyint(4) NOT NULL,
  `team_points` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`result_id`,`team`),
  CONSTRAINT `fk_room_game_result_teams_1` FOREIGN KEY (`result_id`) REFERENCES `room_game_results` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Points a team made as a team in a finished room game, next to its players'' own.';

CREATE TABLE `room_game_result_players` (
  `result_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `team` tinyint(4) NOT NULL,
  `score` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`result_id`,`player_id`),
  KEY `ix_room_game_result_players_1` (`player_id`),
  CONSTRAINT `fk_room_game_result_players_1` FOREIGN KEY (`result_id`) REFERENCES `room_game_results` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='One player''s score in a finished room game.';

CREATE TABLE `highscore_entries` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `furni_id` int(11) NOT NULL,
  `score` int(11) NOT NULL DEFAULT 0,
  `is_win` tinyint(1) NOT NULL DEFAULT 0,
  `scored_at` int(10) unsigned NOT NULL,
  `game_seconds` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `ix_highscore_entries_1` (`furni_id`,`score`),
  CONSTRAINT `fk_highscore_entries_1` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lines of a highscore furni.';

CREATE TABLE `highscore_entry_players` (
  `entry_id` int(11) NOT NULL,
  `position` tinyint(4) NOT NULL DEFAULT 0,
  `player_id` int(11) NOT NULL,
  PRIMARY KEY (`entry_id`,`position`),
  KEY `fk_highscore_entry_players_2` (`player_id`),
  CONSTRAINT `fk_highscore_entry_players_1` FOREIGN KEY (`entry_id`) REFERENCES `highscore_entries` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_highscore_entry_players_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The players of a highscore line.';

CREATE TABLE `snowball_weeks` (
  `week_start` date NOT NULL,
  `processed_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`week_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Weeks whose rewards were paid.';

CREATE TABLE `snowball_week_scores` (
  `week_start` date NOT NULL COMMENT 'The Monday (UTC) of the week the matches ended in.',
  `player_id` int(11) NOT NULL,
  `score` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`week_start`,`player_id`),
  KEY `ix_snowball_week_scores_1` (`week_start`,`score`),
  KEY `fk_snowball_week_scores_1` (`player_id`),
  CONSTRAINT `fk_snowball_week_scores_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What a player scored in Snowball in one week: the weekly boards and prizes.';

CREATE TABLE `snowball_totals` (
  `player_id` int(11) NOT NULL,
  `score` bigint(20) unsigned NOT NULL,
  `games` int(10) unsigned NOT NULL,
  PRIMARY KEY (`player_id`),
  KEY `ix_snowball_totals_1` (`score`),
  CONSTRAINT `fk_snowball_totals_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A player''s Snowball score and finished games over all time: the all-time boards.';

CREATE TABLE `snowball_plays` (
  `player_id` int(11) NOT NULL,
  `day` date NOT NULL,
  `games` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`player_id`,`day`),
  CONSTRAINT `fk_snowball_plays_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The Snowball games a player started on a day.';

CREATE TABLE `player_game_tokens` (
  `player_id` int(11) NOT NULL,
  `tokens` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`player_id`),
  CONSTRAINT `fk_player_game_tokens_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bought Snowball games a player has not played yet.';

CREATE TABLE `room_competitions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL COMMENT 'The name the client asks by and looks its texts up with.',
  `entries_from` int(10) unsigned NOT NULL COMMENT 'Rooms enter from here until voting starts.',
  `voting_from` int(10) unsigned NOT NULL COMMENT 'Votes are cast from here until the end.',
  `ends_at` int(10) unsigned NOT NULL,
  `votes_per_day` smallint(5) unsigned NOT NULL COMMENT 'Votes one player has each day.',
  `rooms_newer_than` int(10) unsigned DEFAULT NULL COMMENT 'Only rooms made after this moment enter; NULL takes any room.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_room_competitions_1` (`code`),
  KEY `ix_room_competitions_1` (`entries_from`,`ends_at`),
  CONSTRAINT `ck_room_competitions_timeline` CHECK (`entries_from` <= `voting_from` AND `voting_from` <= `ends_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A room building competition.';

CREATE TABLE `room_competition_furni` (
  `competition_id` int(11) NOT NULL,
  `furni_code` varchar(70) NOT NULL,
  `position` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`competition_id`,`furni_code`),
  CONSTRAINT `fk_room_competition_furni_1` FOREIGN KEY (`competition_id`) REFERENCES `room_competitions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Furni a competition room must have.';

CREATE TABLE `room_competition_entries` (
  `competition_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `room_id` int(11) DEFAULT NULL COMMENT 'NULL once the room is deleted; the entry keeps the player''s one place.',
  `entered_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`competition_id`,`player_id`),
  UNIQUE KEY `uq_room_competition_entries_1` (`competition_id`,`room_id`),
  KEY `fk_room_competition_entries_2` (`room_id`),
  KEY `fk_room_competition_entries_3` (`player_id`),
  CONSTRAINT `fk_room_competition_entries_1` FOREIGN KEY (`competition_id`) REFERENCES `room_competitions` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_room_competition_entries_2` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_room_competition_entries_3` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The one room each player entered in a competition.';

CREATE TABLE `room_competition_votes` (
  `competition_id` int(11) NOT NULL,
  `entrant_id` int(11) NOT NULL COMMENT 'The player whose entry the vote is for.',
  `voter_id` int(11) NOT NULL,
  `day_number` int(10) unsigned NOT NULL COMMENT 'The UTC day of the vote, counted from 1970-01-01.',
  PRIMARY KEY (`competition_id`,`entrant_id`,`voter_id`),
  KEY `ix_room_competition_votes_1` (`competition_id`,`voter_id`,`day_number`),
  KEY `fk_room_competition_votes_2` (`voter_id`),
  CONSTRAINT `fk_room_competition_votes_1` FOREIGN KEY (`competition_id`,`entrant_id`) REFERENCES `room_competition_entries` (`competition_id`,`player_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_room_competition_votes_2` FOREIGN KEY (`voter_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='One player''s vote for an entry; the standing counts these rows.';

CREATE TABLE `photo_competition_entries` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) NOT NULL,
  `room_id` int(11) DEFAULT NULL,
  `url` varchar(255) NOT NULL DEFAULT '',
  `submitted_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_photo_competition_entries_1` (`player_id`,`submitted_at`),
  CONSTRAINT `fk_photo_competition_entries_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Photos entered in the photo competition. The feature is available and off by default (camera.competition.is_open = 0).';

CREATE TABLE `polls` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `kind` varchar(32) NOT NULL DEFAULT '',
  `title` varchar(255) NOT NULL,
  `headline` varchar(255) NOT NULL DEFAULT '',
  `summary` varchar(512) NOT NULL DEFAULT '',
  `thanks_message` varchar(255) NOT NULL,
  `reward_badge` varchar(32) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A poll or quiz.';

CREATE TABLE `poll_questions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `poll_id` int(11) NOT NULL,
  `parent_id` int(11) DEFAULT NULL,
  `position` int(11) NOT NULL DEFAULT 0,
  `category` int(11) NOT NULL DEFAULT 0,
  `question` varchar(255) NOT NULL,
  `kind` int(11) NOT NULL DEFAULT 2,
  `min_selections` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `ix_poll_questions_1` (`poll_id`,`parent_id`,`position`),
  KEY `fk_poll_questions_2` (`parent_id`),
  CONSTRAINT `fk_poll_questions_1` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_poll_questions_2` FOREIGN KEY (`parent_id`) REFERENCES `poll_questions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A question in a poll.';

CREATE TABLE `poll_question_options` (
  `question_id` int(11) NOT NULL,
  `position` tinyint(4) NOT NULL DEFAULT 0,
  `value` varchar(64) NOT NULL,
  `text` varchar(255) NOT NULL,
  `category` int(11) DEFAULT NULL,
  PRIMARY KEY (`question_id`,`position`),
  CONSTRAINT `fk_poll_question_options_1` FOREIGN KEY (`question_id`) REFERENCES `poll_questions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A choice of a question.';

CREATE TABLE `poll_answers` (
  `poll_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `question_id` int(11) NOT NULL,
  `answer` varchar(255) NOT NULL,
  PRIMARY KEY (`poll_id`,`player_id`,`question_id`),
  KEY `fk_poll_answers_2` (`player_id`),
  CONSTRAINT `fk_poll_answers_1` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_poll_answers_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A player''s answer to a question.';


-- Wired

CREATE TABLE `wired_boxes` (
  `furni_id` int(11) NOT NULL,
  `is_fresh` tinyint(1) NOT NULL DEFAULT 1,
  `ints` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`ints`)),
  `text` text NOT NULL,
  `furni_sources` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`furni_sources`)),
  `player_sources` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`player_sources`)),
  `variable_ids` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`variable_ids`)),
  `quantifier` tinyint(4) NOT NULL DEFAULT 0,
  `delay_half_seconds` tinyint(4) NOT NULL DEFAULT 0,
  `filter_existing` tinyint(1) NOT NULL DEFAULT 0,
  `invert` tinyint(1) NOT NULL DEFAULT 0,
  `extras` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`extras`)),
  PRIMARY KEY (`furni_id`),
  CONSTRAINT `fk_wired_boxes_1` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A wired box''s saved form. Its fields are what the client''s Update message carries; the dialog shows them back as sent.';

CREATE TABLE `wired_box_picks` (
  `box_id` int(11) NOT NULL,
  `list` tinyint(4) NOT NULL DEFAULT 1,
  `position` smallint(6) NOT NULL,
  `furni_id` int(11) NOT NULL,
  `saved_state` varchar(1024) DEFAULT NULL,
  `saved_x` smallint(6) DEFAULT NULL,
  `saved_y` smallint(6) DEFAULT NULL,
  `saved_z` double DEFAULT NULL,
  `saved_direction` tinyint(4) DEFAULT NULL,
  `saved_wall_position` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`box_id`,`list`,`position`),
  KEY `ix_wired_box_picks_1` (`furni_id`),
  CONSTRAINT `fk_wired_box_picks_1` FOREIGN KEY (`box_id`) REFERENCES `wired_boxes` (`furni_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_wired_box_picks_2` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The furni a box has picked, in order, with what it saw at the save for the boxes that compare with it.';

CREATE TABLE `wired_room_settings` (
  `room_id` int(11) NOT NULL,
  `readers` tinyint(3) unsigned NOT NULL,
  `editors` tinyint(3) unsigned NOT NULL,
  `clock_zone` varchar(40) DEFAULT NULL,
  PRIMARY KEY (`room_id`),
  CONSTRAINT `fk_wired_room_settings_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What a room''s owner chose in its wired settings: who may read and who may change its wired (1 everyone, 2 rights holders, 4 group members, 8 group admins, added up) and the time zone its date and time boxes go by (NULL: the game''s). No row: nothing chosen yet.';

CREATE TABLE `wired_furni_values` (
  `room_id` int(11) NOT NULL,
  `furni_id` int(11) NOT NULL,
  `variable_box_id` int(11) NOT NULL,
  `value` bigint(20) DEFAULT NULL,
  `created_at_ms` bigint(20) NOT NULL,
  `updated_at_ms` bigint(20) NOT NULL,
  PRIMARY KEY (`room_id`,`furni_id`,`variable_box_id`),
  KEY `ix_wired_furni_values_1` (`room_id`,`variable_box_id`),
  KEY `ix_wired_furni_values_2` (`furni_id`),
  CONSTRAINT `fk_wired_furni_values_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_wired_furni_values_2` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A furni variable''s value.';

CREATE TABLE `wired_player_values` (
  `room_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `variable_box_id` int(11) NOT NULL,
  `value` bigint(20) DEFAULT NULL,
  `created_at_ms` bigint(20) NOT NULL,
  `updated_at_ms` bigint(20) NOT NULL,
  PRIMARY KEY (`room_id`,`player_id`,`variable_box_id`),
  KEY `ix_wired_player_values_1` (`room_id`,`variable_box_id`),
  KEY `ix_wired_player_values_2` (`player_id`),
  CONSTRAINT `fk_wired_player_values_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_wired_player_values_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A player variable''s value.';

CREATE TABLE `wired_room_values` (
  `room_id` int(11) NOT NULL,
  `variable_box_id` int(11) NOT NULL,
  `value` bigint(20) NOT NULL DEFAULT 0,
  `created_at_ms` bigint(20) NOT NULL,
  `updated_at_ms` bigint(20) NOT NULL,
  PRIMARY KEY (`room_id`,`variable_box_id`),
  CONSTRAINT `fk_wired_room_values_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A room variable''s value.';

CREATE TABLE `wired_link_networks` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_wired_link_networks_1` (`player_id`,`name`),
  CONSTRAINT `fk_wired_link_networks_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A player''s named link networks.';

CREATE TABLE `wired_link_members` (
  `furni_id` int(11) NOT NULL,
  `network_id` int(11) DEFAULT NULL,
  `hears_own_room` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`furni_id`),
  KEY `ix_wired_link_members_1` (`network_id`),
  CONSTRAINT `fk_wired_link_members_1` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_wired_link_members_2` FOREIGN KEY (`network_id`) REFERENCES `wired_link_networks` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A Link''s setup: its network, if any, and whether it hears its own room.';

CREATE TABLE `wired_spawned_furni` (
  `furni_id` int(11) NOT NULL,
  `room_id` int(11) NOT NULL,
  PRIMARY KEY (`furni_id`),
  KEY `ix_wired_spawned_furni_1` (`room_id`),
  CONSTRAINT `fk_wired_spawned_furni_1` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_wired_spawned_furni_2` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Furni the Place furni box made, and the room it made them in.';

CREATE TABLE `wired_chests` (
  `furni_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL DEFAULT '',
  `description` varchar(255) NOT NULL DEFAULT '',
  `credits` int(11) NOT NULL DEFAULT 0,
  `capacity` int(11) NOT NULL DEFAULT 0,
  `capacity_max` int(11) NOT NULL DEFAULT 0,
  `appearance` tinyint(4) NOT NULL DEFAULT 0,
  `preview_mode` tinyint(4) NOT NULL DEFAULT 0,
  `preview_amount` int(11) NOT NULL DEFAULT 1,
  `placed` tinyint(1) NOT NULL DEFAULT 1,
  `access_open` tinyint(1) NOT NULL DEFAULT 0,
  `access_donate` tinyint(1) NOT NULL DEFAULT 0,
  `locked` tinyint(1) NOT NULL DEFAULT 0,
  `auto_lock` tinyint(1) NOT NULL DEFAULT 0,
  `wired_enabled` tinyint(1) NOT NULL DEFAULT 0,
  `notify_mode` tinyint(4) NOT NULL DEFAULT 0,
  `notify_full` tinyint(1) NOT NULL DEFAULT 0,
  `notify_donation` tinyint(1) NOT NULL DEFAULT 0,
  `notify_withdraw` tinyint(1) NOT NULL DEFAULT 0,
  `notify_empty` tinyint(1) NOT NULL DEFAULT 0,
  `notify_wired` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`furni_id`),
  CONSTRAINT `fk_wired_chests_1` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A wired chest: its settings and its coins.';

CREATE TABLE `wired_chest_furni` (
  `chest_id` int(11) NOT NULL,
  `slot_id` int(11) NOT NULL,
  `transaction_id` bigint(20) NOT NULL DEFAULT 0,
  `definition_id` int(11) NOT NULL,
  `sprite_id` int(11) NOT NULL DEFAULT 0,
  `is_wall` tinyint(1) NOT NULL DEFAULT 0,
  `groupable` tinyint(1) NOT NULL DEFAULT 1,
  `state` varchar(1024) NOT NULL DEFAULT '',
  `poster_code` varchar(32) NOT NULL DEFAULT '',
  `limited_number` int(11) NOT NULL DEFAULT 0,
  `limited_total` int(11) NOT NULL DEFAULT 0,
  `extra` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`chest_id`,`slot_id`),
  KEY `fk_wired_chest_furni_2` (`definition_id`),
  CONSTRAINT `fk_wired_chest_furni_1` FOREIGN KEY (`chest_id`) REFERENCES `wired_chests` (`furni_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_wired_chest_furni_2` FOREIGN KEY (`definition_id`) REFERENCES `furni_definitions` (`id`) ON DELETE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Furni inside a chest. A furni in a chest is no longer a furni row: it keeps only what comes back out.';

CREATE TABLE `wired_chest_log` (
  `id` bigint(20) NOT NULL,
  `room_id` int(11) NOT NULL,
  `chest_id` int(11) NOT NULL,
  `holds_credits` tinyint(1) NOT NULL,
  `cause` tinyint(4) NOT NULL,
  `label` varchar(255) NOT NULL DEFAULT '',
  `player_id` int(11) DEFAULT NULL,
  `taken` int(11) NOT NULL DEFAULT 0,
  `added` int(11) NOT NULL DEFAULT 0,
  `logged_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_wired_chest_log_1` (`room_id`,`logged_at`),
  KEY `ix_wired_chest_log_2` (`chest_id`,`logged_at`),
  CONSTRAINT `fk_wired_chest_log_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A chest''s history, an entry for each move in or out; the server numbers the entries.';

CREATE TABLE `wired_chest_log_furni` (
  `entry_id` bigint(20) NOT NULL,
  `added` tinyint(1) NOT NULL,
  `is_wall` tinyint(1) NOT NULL,
  `sprite_id` int(11) NOT NULL,
  `amount` int(11) NOT NULL,
  PRIMARY KEY (`entry_id`,`added`,`is_wall`,`sprite_id`),
  CONSTRAINT `fk_wired_chest_log_furni_1` FOREIGN KEY (`entry_id`) REFERENCES `wired_chest_log` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The furni one chest log entry put in or took out, a row for each kind.';

CREATE TABLE `wired_chest_notices` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) NOT NULL,
  `chest_id` int(11) NOT NULL,
  `room_id` int(11) NOT NULL,
  `reason` tinyint(4) NOT NULL DEFAULT 0,
  `chest_name` varchar(100) NOT NULL DEFAULT '',
  `actor_id` int(11) NOT NULL DEFAULT 0 COMMENT '0: nobody, such as earnings.',
  `amount` int(11) NOT NULL DEFAULT 0,
  `created_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_wired_chest_notices_1` (`player_id`,`id`),
  CONSTRAINT `fk_wired_chest_notices_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chest notices waiting for a player to log in.';

CREATE TABLE `wired_contracts` (
  `furni_id` int(11) NOT NULL,
  `payment_mode` tinyint(4) NOT NULL DEFAULT 0,
  `receive_text` varchar(255) NOT NULL DEFAULT '',
  `layout` enum('generic','games') NOT NULL DEFAULT 'generic',
  `reward_category` int(11) NOT NULL DEFAULT 11,
  `show_reward_popup` tinyint(1) NOT NULL DEFAULT 1,
  `reward_text` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`furni_id`),
  CONSTRAINT `fk_wired_contracts_1` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A contract furni''s rules.';

CREATE TABLE `wired_contract_terms` (
  `contract_id` int(11) NOT NULL,
  `side` tinyint(4) NOT NULL DEFAULT 0,
  `rule` tinyint(4) NOT NULL DEFAULT 0,
  `position` tinyint(4) NOT NULL DEFAULT 0,
  `is_credits` tinyint(1) NOT NULL DEFAULT 0,
  `amount` int(11) NOT NULL DEFAULT 0,
  `definition_id` int(11) DEFAULT NULL,
  `unknown_sprite_id` int(11) DEFAULT NULL,
  `poster_code` varchar(32) NOT NULL DEFAULT '',
  PRIMARY KEY (`contract_id`,`side`,`rule`,`position`),
  KEY `fk_wired_contract_terms_2` (`definition_id`),
  CONSTRAINT `fk_wired_contract_terms_1` FOREIGN KEY (`contract_id`) REFERENCES `wired_contracts` (`furni_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_wired_contract_terms_2` FOREIGN KEY (`definition_id`) REFERENCES `furni_definitions` (`id`) ON DELETE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='One line of what a contract gives or gets.';

CREATE TABLE `wired_api_keys` (
  `box_id` int(11) NOT NULL,
  `kind` enum('read','write') NOT NULL,
  `room_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `key_hash` binary(32) NOT NULL,
  `key_hint` char(4) NOT NULL,
  `key_sealed` varbinary(96) DEFAULT NULL,
  `created_at_ms` bigint(20) NOT NULL,
  PRIMARY KEY (`box_id`,`kind`),
  UNIQUE KEY `uq_wired_api_keys_1` (`key_hash`),
  KEY `ix_wired_api_keys_1` (`room_id`,`kind`),
  KEY `fk_wired_api_keys_3` (`player_id`),
  CONSTRAINT `fk_wired_api_keys_1` FOREIGN KEY (`box_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_wired_api_keys_2` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_wired_api_keys_3` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Keys of the web API boxes; only the hash and a sealed copy are stored.';

CREATE TABLE `wired_reward_claims` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `furni_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `reward_code` varchar(100) NOT NULL,
  `given_at` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_wired_rewards_given_1` (`furni_id`,`player_id`),
  KEY `ix_wired_rewards_given_2` (`player_id`),
  CONSTRAINT `fk_wired_rewards_given_1` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_wired_rewards_given_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A reward a give reward box gave a player, and when (unix seconds).';


-- Moderation

CREATE TABLE `sanction_steps` (
  `step` int(11) NOT NULL,
  `kind` enum('ALERT','MUTE','BAN') NOT NULL,
  `hours` int(11) NOT NULL DEFAULT 0 COMMENT 'How long the mute or ban lasts; 0 for an alert.',
  `probation_days` int(11) NOT NULL DEFAULT 0 COMMENT 'How long the step is remembered: a sanction within it is the next step.',
  PRIMARY KEY (`step`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The sanction ladder, step 1 first; the top step repeats.';

CREATE TABLE `sanctions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) NOT NULL,
  `step` int(11) NOT NULL,
  `kind` enum('ALERT','MUTE','BAN') NOT NULL COMMENT 'What the step was when it was given.',
  `reason` varchar(255) NOT NULL DEFAULT '',
  `topic_id` int(11) DEFAULT NULL,
  `issued_by` int(11) DEFAULT NULL,
  `issued_at` int(10) unsigned NOT NULL,
  `ends_at` int(10) unsigned DEFAULT NULL COMMENT 'When the mute or ban ends; NULL for an alert.',
  `probation_ends_at` int(10) unsigned DEFAULT NULL COMMENT 'NULL: the probation is over.',
  PRIMARY KEY (`id`),
  KEY `ix_sanctions_1` (`player_id`,`id`),
  KEY `fk_sanctions_2` (`issued_by`),
  CONSTRAINT `fk_sanctions_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_sanctions_2` FOREIGN KEY (`issued_by`) REFERENCES `players` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The ladder steps a player was given; the newest one says where they stand.';

CREATE TABLE `bans` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) NOT NULL,
  `issued_by` int(11) DEFAULT NULL,
  `issued_at` int(10) unsigned NOT NULL,
  `ends_at` int(10) unsigned NOT NULL COMMENT 'Lifting a ban moves this to the moment it was lifted.',
  `lifted_by` int(11) DEFAULT NULL COMMENT 'Who ended it before its time; NULL while it runs and when it ran out.',
  `reason` varchar(200) NOT NULL DEFAULT '',
  `topic_id` int(11) DEFAULT NULL,
  `barred_address` varchar(45) DEFAULT NULL COMMENT 'An address the ban keeps out as well, whoever logs in from it; NULL for none.',
  `barred_machine` varchar(64) DEFAULT NULL COMMENT 'A machine id the ban keeps out as well; NULL for none.',
  PRIMARY KEY (`id`),
  KEY `ix_bans_1` (`player_id`,`ends_at`),
  KEY `ix_bans_2` (`barred_address`,`ends_at`),
  KEY `ix_bans_3` (`barred_machine`,`ends_at`),
  KEY `fk_bans_2` (`issued_by`),
  KEY `fk_bans_3` (`topic_id`),
  KEY `fk_bans_4` (`lifted_by`),
  CONSTRAINT `fk_bans_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_bans_2` FOREIGN KEY (`issued_by`) REFERENCES `players` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_bans_3` FOREIGN KEY (`topic_id`) REFERENCES `report_topics` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_bans_4` FOREIGN KEY (`lifted_by`) REFERENCES `players` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bans, each on one player until its end.';

CREATE TABLE `device_bans` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `p_id` char(16) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT 'The random id the browser keeps.',
  `p_gpu` char(8) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `p_scr` char(8) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `p_cpu` char(8) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `p_tz` char(8) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `p_fnt` char(8) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `p_cnv` char(8) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `p_aud` char(8) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `p_gl` char(8) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `p_ua` char(8) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `reason` varchar(255) NOT NULL DEFAULT '',
  `staff_id` int(11) NOT NULL,
  `created_at` int(10) unsigned NOT NULL,
  `expires_at` int(10) unsigned DEFAULT NULL COMMENT 'NULL: no end.',
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_device_bans_1` CHECK (
    `p_id` IS NOT NULL OR
    (`p_gpu` IS NOT NULL) + (`p_scr` IS NOT NULL) + (`p_cpu` IS NOT NULL) + (`p_tz` IS NOT NULL) +
    (`p_fnt` IS NOT NULL) + (`p_cnv` IS NOT NULL) + (`p_aud` IS NOT NULL) + (`p_gl` IS NOT NULL) +
    (`p_ua` IS NOT NULL) >= 3
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Device patterns staff ban: a login whose pieces equal every column that is not NULL is refused. Without the id a pattern names at least three pieces.';

CREATE TABLE `device_ban_exemptions` (
  `player_id` int(11) NOT NULL,
  `staff_id` int(11) NOT NULL,
  `created_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`player_id`),
  CONSTRAINT `fk_device_ban_exemptions_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Players no device ban refuses; account, machine and address bans still do.';

CREATE TABLE `word_filters` (
  `word` varchar(191) NOT NULL,
  `replacement` varchar(16) DEFAULT '***' COMMENT 'What stands in the word''s place; NULL: a line with the word is not said at all.',
  `mute_seconds` int(11) NOT NULL DEFAULT 0 COMMENT 'Above 0 the word also mutes whoever wrote it.',
  PRIMARY KEY (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Words the game filters.';

CREATE TABLE `report_categories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `internal_name` varchar(255) DEFAULT NULL,
  `display_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Top level of the call-for-help form.';

CREATE TABLE `report_topics` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `category_id` int(11) NOT NULL,
  `internal_name` varchar(255) DEFAULT NULL,
  `display_name` varchar(255) DEFAULT NULL,
  `sort_order` int(11) NOT NULL DEFAULT 0,
  `goes_to` enum('moderators','guardians','nobody') NOT NULL DEFAULT 'moderators' COMMENT 'Who looks at a report on this topic; nobody: it closes itself with the reply.',
  `is_urgent` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Its tickets come before the others on the moderators'' desk.',
  `ignores_accused` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'A report that closes itself makes the reporter ignore the accused.',
  `reply` text DEFAULT NULL COMMENT 'What the reporter is told at once.',
  `reply_without_link` tinyint(1) NOT NULL DEFAULT 0,
  `preset_id` int(11) DEFAULT NULL COMMENT 'The ready-made sanction that goes with the topic.',
  PRIMARY KEY (`id`),
  KEY `fk_report_topics_1` (`category_id`),
  KEY `fk_report_topics_2` (`preset_id`),
  CONSTRAINT `fk_report_topics_1` FOREIGN KEY (`category_id`) REFERENCES `report_categories` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_report_topics_2` FOREIGN KEY (`preset_id`) REFERENCES `issue_presets` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A report reason.';

CREATE TABLE `support_tickets` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `kind` tinyint(4) NOT NULL DEFAULT 1 COMMENT 'How it was filed: the number the moderation tool shows.',
  `status` enum('open','picked','closed') NOT NULL DEFAULT 'open',
  `resolution` enum('withdrawn','useless','abusive','handled','automatic') DEFAULT NULL COMMENT 'Why it was closed; NULL while it is open or picked.',
  `created_at` int(10) unsigned NOT NULL,
  `priority` int(11) NOT NULL DEFAULT 10 COMMENT 'The lowest number is handed out first.',
  `reporter_id` int(11) DEFAULT NULL,
  `accused_id` int(11) DEFAULT NULL,
  `room_id` int(11) DEFAULT NULL,
  `message` varchar(500) NOT NULL DEFAULT '',
  `topic_id` int(11) DEFAULT NULL,
  `guild_id` int(11) DEFAULT NULL,
  `forum_thread_id` int(11) DEFAULT NULL,
  `forum_post_id` int(11) DEFAULT NULL,
  `photo_furni_id` int(11) DEFAULT NULL,
  `moderator_id` int(11) DEFAULT NULL,
  `released` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'A moderator picked it and let it go again.',
  `closed_at` int(10) unsigned DEFAULT NULL,
  `appeal` enum('none','pending','upheld','rejected') NOT NULL DEFAULT 'none',
  `appealed_at` int(10) unsigned DEFAULT NULL,
  `appeal_resolved_at` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_support_tickets_1` (`status`),
  KEY `ix_support_tickets_2` (`created_at`),
  KEY `ix_support_tickets_3` (`reporter_id`,`created_at`),
  KEY `ix_support_tickets_4` (`accused_id`),
  KEY `ix_support_tickets_5` (`room_id`),
  KEY `fk_support_tickets_4` (`moderator_id`),
  CONSTRAINT `fk_support_tickets_1` FOREIGN KEY (`reporter_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_support_tickets_4` FOREIGN KEY (`moderator_id`) REFERENCES `players` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A report or help request.';

CREATE TABLE `support_ticket_lines` (
  `ticket_id` int(11) NOT NULL,
  `line_no` int(11) NOT NULL DEFAULT 0,
  `player_id` int(11) NOT NULL,
  `message` varchar(500) NOT NULL DEFAULT '',
  PRIMARY KEY (`ticket_id`,`line_no`),
  CONSTRAINT `fk_support_ticket_lines_1` FOREIGN KEY (`ticket_id`) REFERENCES `support_tickets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chat lines attached to a ticket.';

CREATE TABLE `issue_categories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Folders of a moderator''s preset list.';

CREATE TABLE `issue_presets` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `category_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL DEFAULT '',
  `message` varchar(300) NOT NULL DEFAULT '' COMMENT 'What the sanctioned player is told.',
  `ban_hours` int(11) NOT NULL DEFAULT 0,
  `mute_hours` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `fk_issue_presets_1` (`category_id`),
  CONSTRAINT `fk_issue_presets_1` FOREIGN KEY (`category_id`) REFERENCES `issue_categories` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A moderator''s ready-made sanction.';

CREATE TABLE `moderator_presets` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `kind` enum('user','room') NOT NULL DEFAULT 'user',
  `text` varchar(200) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Ready-made texts for moderators.';

CREATE TABLE `room_chat_messages` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `room_id` int(11) NOT NULL,
  `sender_id` int(11) NOT NULL,
  `target_id` int(11) DEFAULT NULL,
  `message` varchar(255) NOT NULL,
  `sent_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_room_chat_messages_1` (`room_id`,`sent_at`),
  KEY `ix_room_chat_messages_2` (`sender_id`,`sent_at`),
  KEY `ix_room_chat_messages_3` (`target_id`),
  KEY `ix_room_chat_messages_4` (`sent_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Room chat for moderators and staff pages.';


CREATE TABLE `command_runs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `ran_at` int(10) unsigned NOT NULL,
  `player_id` int(11) NOT NULL,
  `command` varchar(64) NOT NULL,
  `line` varchar(255) NOT NULL,
  `was_applied` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_command_runs_1` (`player_id`,`ran_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chat commands of a rank with log_commands on: the command and the line as typed.';

CREATE TABLE `staff_actions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `acted_at` int(10) unsigned NOT NULL,
  `staff_id` int(11) NOT NULL COMMENT 'The player who acted; their name is read from players when the log is shown.',
  `action` varchar(64) NOT NULL,
  `subject_type` varchar(16) NOT NULL DEFAULT '' COMMENT 'What subject_id names: user, room, ticket, news and the like.',
  `subject_id` int(11) NOT NULL DEFAULT 0,
  `subject_label` varchar(128) NOT NULL DEFAULT '' COMMENT 'What the subject was called then.',
  `detail` varchar(500) NOT NULL DEFAULT '',
  `success` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `ix_staff_actions_1` (`subject_type`,`subject_id`,`id`),
  KEY `ix_staff_actions_2` (`staff_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='What staff did through the housekeeping pages.';


-- Web: what the web pages show.

CREATE TABLE `news_articles` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL,
  `body` varchar(500) NOT NULL,
  `button_text` varchar(50) NOT NULL,
  `button_kind` enum('catalog','room','client','web','none') NOT NULL DEFAULT 'none',
  `button_link` varchar(200) NOT NULL,
  `image` varchar(200) NOT NULL,
  `sort_order` int(11) NOT NULL DEFAULT 0,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Articles the client''s landing view shows.';

CREATE TABLE `news_panels` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(150) NOT NULL,
  `body` text NOT NULL,
  `image` mediumtext DEFAULT NULL,
  `link_text` varchar(80) NOT NULL DEFAULT '',
  `link_url` varchar(255) NOT NULL DEFAULT '',
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `sort_order` int(11) NOT NULL DEFAULT 0,
  `created_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_news_panels_1` (`enabled`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Panels the web front page shows.';

CREATE TABLE `web_session_revocations` (
  `nonce` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'The session''s random part from its cookie (base64url, so case counts).',
  `expires_at` int(10) unsigned NOT NULL COMMENT 'When the session would have expired (unix seconds); the row may go after that.',
  PRIMARY KEY (`nonce`),
  KEY `ix_web_session_revocations_1` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Web sessions ended by logging out, written and read by the web only.';

CREATE TABLE `help_requests` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) DEFAULT NULL COMMENT 'The logged-in player who sent it; NULL for a visitor without an account.',
  `name` varchar(50) NOT NULL DEFAULT '' COMMENT 'The name a visitor gave (their player name, if they have one); empty for a player.',
  `contact` varchar(254) NOT NULL DEFAULT '' COMMENT 'The e-mail address a visitor gave for the answer; empty for a player, who reads the answer on the site.',
  `topic` varchar(16) NOT NULL COMMENT 'account, bug, safety, idea or other.',
  `message` text NOT NULL COMMENT 'What they wrote, at most 2000 characters.',
  `ip` varchar(45) NOT NULL DEFAULT '' COMMENT 'The address it came from, for staff and the rate limit.',
  `created_at` int(10) unsigned NOT NULL,
  `state` tinyint(4) NOT NULL DEFAULT 1 COMMENT '1 open, 2 picked by a staff member, 0 closed.',
  `staff_id` int(11) DEFAULT NULL COMMENT 'The staff member who picked it, answered it or closed it last.',
  `closed_at` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_help_requests_1` (`state`,`id`),
  KEY `ix_help_requests_2` (`player_id`,`created_at`),
  KEY `ix_help_requests_3` (`ip`,`created_at`),
  KEY `fk_help_requests_2` (`staff_id`),
  CONSTRAINT `fk_help_requests_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_help_requests_2` FOREIGN KEY (`staff_id`) REFERENCES `players` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Help requests from the website, answered in housekeeping.';

CREATE TABLE `help_request_answers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `request_id` int(11) NOT NULL,
  `staff_id` int(11) DEFAULT NULL COMMENT 'The staff member who wrote it.',
  `message` text NOT NULL COMMENT 'The answer, at most 2000 characters.',
  `created_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_help_request_answers_1` (`request_id`,`id`),
  KEY `fk_help_request_answers_2` (`staff_id`),
  CONSTRAINT `fk_help_request_answers_1` FOREIGN KEY (`request_id`) REFERENCES `help_requests` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_help_request_answers_2` FOREIGN KEY (`staff_id`) REFERENCES `players` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Staff answers to help requests from the website.';

SET foreign_key_checks = 1;


-- Base data: settings, texts, ranks and their rights, the sanction ladder, one room model and the room category
-- new rooms go in.

-- settings. Server settings an operator changes in the database; game rules live in config/game-rules.conf.
INSERT INTO `settings` (`name`, `value`, `description`) VALUES
  ('bots.butler.hearing.tiles','5','How far away, in tiles, a butler still hears an order.'),
  ('bots.butler.reach.tiles','3','How near, in tiles, a roaming butler hands an order over from where it stands; farther off it carries it over.'),
  ('bots.name.max.characters','15','Longest name a bot may be given.'),
  ('bots.roaming.radius.tiles','3','How far, in tiles, a roaming bot strolls from the tile it was placed on; 0 is the whole room.'),
  ('bots.speech.max_gap.seconds','604800','Longest pause a bot may be given between two of its lines.'),
  ('bots.speech.min_gap.seconds','5','Shortest pause a bot may be given between two of its lines.'),
  ('bots.visitor_log.time_format','yyyy-MM-dd HH:mm','Pattern the visitor log bot writes visit times with (java.time letters).'),
  ('calendar.campaign','','Name of the calendar campaign that is open; empty for none.'),
  ('calendar.daily_offer.offer_id','0','Catalogue offer the calendar shows as its daily offer; 0 for none.'),
  ('calendar.is_open','0','The campaign calendar opens for players at login.'),
  ('camera.competition.entries_per_day','3','Photo competition entries one player may send a day; at least 1.'),
  ('camera.competition.is_open','0','The camera takes photo competition entries.'),
  ('camera.photo.furni_id','45970','Furni definition a bought photo becomes.'),
  ('camera.photo.price.credits','2','Credits a photo costs.'),
  ('camera.photo.price.points','0','Activity points a photo costs on top of the credits.'),
  ('camera.photo.price.points_type','0','Activity point type the photo price is taken in.'),
  ('camera.publish.price.points','10','Activity points it costs to publish a photo.'),
  ('camera.publish.price.points_type','0','Activity point type the publish price is taken in.'),
  ('catalog.bundle_discount.batch_size','0','Copies that make one discount batch; 0 gives no discount.'),
  ('catalog.bundle_discount.bonus.min_batches','-1','Batches an order needs before the bonus copies count; -1 is never.'),
  ('catalog.bundle_discount.bonus.thresholds','5;9;9;13;13;17;17;21;21;25;25;29;29;33;33;37;37;41;41;45;45;49;49;53;53;57;57;61;61;65;65;69;69;73;73;77;77;81;81;85;85;89;89;93;93;97;97','Order sizes at which one more copy is free, separated by semicolons.'),
  ('catalog.bundle_discount.free_per_batch','0','Copies of each batch that are free.'),
  ('catalog.bundle_discount.max_items','100','Most copies one discounted order may hold.'),
  ('catalog.engraving.max.characters','300','Longest text engraved on a trophy.'),
  ('catalog.gifts.box_types','0,1,2,3,4,5,6,8','Gift box types the catalogue offers, comma separated.'),
  ('catalog.gifts.note.max.characters','140','Longest note a gift carries; the client''s note field takes 140.'),
  ('catalog.gifts.ribbon_types','0,1,2,3,4,5,6,7,8,9,10','Gift ribbon types the catalogue offers, comma separated.'),
  ('catalog.gifts.wrapping.price.credits','1','Credits a gift box other than the plain one costs.'),
  ('catalog.limited.daily_cap.is_on','1','A player may buy only so many limited editions a day.'),
  ('catalog.limited.daily_cap.overall','10','Limited editions of any kind a player may buy a day; 0 is no cap.'),
  ('catalog.limited.daily_cap.per_offer','3','Copies of one limited edition a player may buy a day; 0 is no cap.'),
  ('catalog.limited.raffle.is_on','1','The copies of a newly released limited edition are drawn among the buyers of its first moments.'),
  ('catalog.limited.raffle.window.seconds','10','Seconds a limited edition raffle collects buyers before it draws.'),
  ('catalog.offers.sorted_by_position','1','A page lists its offers by their position; off lists them by id.'),
  ('catalog.purchase.gap.seconds','1','Seconds a player waits between two purchases.'),
  ('catalog.room_bundles.include_bots','1','A bought room bundle comes with the bots of its template room.'),
  ('catalog.target_offer.id','1','Targeted offer the client is shown; 0 for none.'),
  ('chat.flood.counts_rights_holders','0','The flood filter also counts what players with rights in the room say.'),
  ('chat.mute_area.passes_whispers','1','A whisper still goes out from inside a mute area.'),
  ('chat.room_lines.are_kept','1','Room chat lines are kept for the chat logs moderators read.'),
  ('client.info_feed.is_on','1','The client shows the notifications of its info feed.'),
  ('club.expiry_check.interval.seconds','10','Seconds between two checks for memberships that ran out and for a pay day that is due.'),
  ('club.payday.currency','-1','Currency the club pay day pays in: -1 is credits, otherwise an activity point type.'),
  ('club.payday.day_of_month','15','Day of the month the club pay day falls on, 1 to 28; it is paid at the start of that day in game time.'),
  ('club.payday.kickback.percent','10','Share of a member''s spending the pay day gives back.'),
  ('club.payday.spending_restarts_with_membership','1','Spending counts for the pay day from the start of a new membership, not from the start of the pay period.'),
  ('club.payday.streak.rewards','7=5;30=10;60=15;90=20;180=25;365=30','What the pay day adds for unbroken membership: days=amount pairs separated by semicolons; the longest length reached counts.'),
  ('club.renewal_discount.before_end.days','7','Days before a membership ends from which the discounted renewal is offered.'),
  ('club.renewal_discount.is_on','1','Members near the end of their membership are offered a discounted renewal.'),
  ('community_goal.concurrent_players.badge_code','ConcurrentUsersReward','Badge handed out when the goal of players online at once is reached.'),
  ('community_goal.concurrent_players.target','0','Players online at once the landing view''s goal asks for; 0 hides the goal.'),
  ('daily_tasks.per_day','3','Daily tasks a player is given each day.'),
  ('database.connection.silent.seconds','60','Seconds a database connection may answer nothing before it is given up.'),
  ('database.pool.check.ms','0','Longest a database connection check may take; 0 keeps the pool''s own.'),
  ('database.pool.idle.ms','0','Idle time after which a spare database connection closes; 0 keeps the pool''s own.'),
  ('database.pool.idle_connections','10','Idle database connections kept open; read at start.'),
  ('database.pool.leak_report.ms','0','A database connection held longer than this is reported; 0 is off.'),
  ('database.pool.lifetime.ms','0','Age at which a database connection is replaced; 0 keeps the pool''s own.'),
  ('database.pool.max_connections','50','Most open database connections; read at start.'),
  ('database.pool.wait.ms','0','Longest wait for a free database connection; 0 keeps the pool''s own.'),
  ('database.writer.queue.per_thread','5000','Writes that may wait for each writer thread; with the queue full the caller waits.'),
  ('database.writer.threads','0','Database writer threads, at most the pool''s connections; 0 takes a third of the pool, two at least.'),
  ('featured_looks.cache.seconds','300','Seconds the featured looks are kept before they are read again.'),
  ('freeze.frozen.boost_lost','0','Extra reach a player loses when frozen.'),
  ('freeze.frozen.duration.seconds','5','Seconds a player stays frozen.'),
  ('freeze.frozen.points_lost','10','Points a player loses when frozen.'),
  ('freeze.frozen.snowballs_lost','5','Extra snowballs a player loses when frozen.'),
  ('freeze.points.per_broken_block','0','Points for breaking an ice block.'),
  ('freeze.points.per_power_up','0','Points for picking up a power-up.'),
  ('freeze.power_up.chance.percent','50','Chance that a broken ice block leaves a power-up.'),
  ('freeze.power_up.max_lives','3','Most lives power-ups bring a player to.'),
  ('freeze.power_up.max_snowballs','5','Most snowballs a player holds at once.'),
  ('freeze.shield.duration.seconds','10','Seconds a shield protects.'),
  ('freeze.shield.time_adds_up','1','A shield picked up while one is running adds its time.'),
  ('furni.data.assets.folder','','Folder that holds the furni data of the installed assets; set from ZEL_DATA_DIR at every start.'),
  ('furni.data.location','','A furni data file or folder to read when the assets folder has none.'),
  ('furni.data.max.bytes','67108864','Largest furni data file the server reads.'),
  ('furni.data.names.are_read','true','Furni take their display names from the furni data.'),
  ('furni.dimmer.takes_palette_only','1','A room dimmer takes only the colours of the client''s palette.'),
  ('furni.jukebox.tracks.large','20','Tracks a large jukebox holds.'),
  ('furni.jukebox.tracks.normal','10','Tracks a jukebox holds.'),
  ('furni.plant.dries_out.hours','24','Hours without water after which a plant furni dies.'),
  ('furni.plant.watering.gap.seconds','1800','Seconds before a watered plant furni may be watered again.'),
  ('furni.pyramid.max_pause.seconds','18','Longest time, in seconds, a pyramid stays up or down.'),
  ('furni.pyramid.min_pause.seconds','1','Shortest time, in seconds, a pyramid stays up or down.'),
  ('furni.rentable_space.rent.days','7','Days one rent of a rentable space lasts.'),
  ('furni.rentable_space.tile.credits','10','Credits one rent of a rentable space costs for each of its tiles.'),
  ('furni.sticky_note.max.characters','366','Longest text a sticky note holds.'),
  ('furni.talking.hearing.tiles','1','How near, in tiles, a speaker must stand for a talking furni to answer; diagonals count as one.'),
  ('furni.teleport.enters_locked_rooms','1','A teleport takes its player into a locked room.'),
  ('game.daily_reset.interval.hours','18','Hours after which the daily respects, the daily pet scratches and the daily tasks start over together; the rounds are counted from 1970-01-01 00:00 UTC, so 24 gives days that turn at UTC midnight.'),
  ('game.name','Game','The game''s name as the client and the site show it; ZEL_GAME_NAME sets it at every start.'),
  ('game.time_zone','','Time zone of the dates and times players see, as an IANA name such as Europe/Amsterdam; empty takes the machine''s.'),
  ('groups.badges.are_drawn','1','The server draws a group''s badge image when the badge is made or changed.'),
  ('groups.badges.output.folder','','Folder the drawn group badges are written to; set from ZEL_DATA_DIR at every start.'),
  ('groups.badges.parts.folder','','Folder with the badge part images; set from ZEL_DATA_DIR at every start.'),
  ('groups.founding.price.credits','10','Credits it costs to found a group.'),
  ('landing.limited_countdown.ends_at','1519496132','Moment the next limited edition appears, in unix seconds; until then the client counts down.'),
  ('landing.limited_countdown.furni_id','10388','Furni definition shown once that moment has passed.'),
  ('landing.limited_countdown.furni_name','trophy_netsafety_0','Furni name shown once that moment has passed.'),
  ('landing.limited_countdown.is_shown','1','The landing view answers the client''s question for the next limited edition.'),
  ('landing.limited_countdown.page_id','13','Catalogue page the shown furni opens.'),
  ('login.client_releases','any','Client version numbers accepted at login, comma-separated; any accepts every version the game has ids for.'),
  ('login.key_exchange.is_required','0','A login that skipped the key exchange is refused while a key is set. The exchange only scrambles the traffic and adds no security of its own: TLS protects a connection.'),
  ('login.ticket.failures.per_address','30','Failed login tickets one client address may have counting against it before its logins are turned away unread; 0 counts none.'),
  ('login.ticket.failures.window.seconds','120','Seconds over which that many failed login tickets are forgiven again, one at a time.'),
  ('login.ticket.is_reusable','0','A login ticket stays valid after its first use; development only, ZEL_DEBUG_LOGIN sets it.'),
  ('maintenance.duration.minutes','15','How long the client is told the maintenance lasts.'),
  ('maintenance.is_active','0','The game is under maintenance: only staff get in.'),
  ('maintenance.lowest_rank_in','5','Lowest rank that still gets in during maintenance and after closing time.'),
  ('marketplace.fee.buyer_adds.percent','1','Mode buyer_adds: percent of the listed price the buyer pays on top, rounded up.'),
  ('marketplace.fee.mode','buyer_adds','Who carries the marketplace fee: buyer_adds (the price typed is the seller''s, the buyer pays a percentage on top) or seller_pays (the price typed is the buyer''s, the seller gets it less a fee).'),
  ('marketplace.fee.seller_pays.percent','2','Mode seller_pays: percent of the listed price kept from the seller.'),
  ('marketplace.fee.seller_pays.price_limit','400000','Mode seller_pays: the fee also grows with the price, by half the price''s share of this number; 0 for no growth.'),
  ('marketplace.price.currency','-1','Currency of marketplace prices and taxes: -1 is credits, otherwise an activity point type.'),
  ('messenger.search.max_results','50','Most players a friend search lists.'),
  ('moderation.caution.link','','Link to the rules a caution sends along; empty sends the caution without one.'),
  ('moderation.mute.max.hours','168','Longest mute one action of a moderator gives, in hours.'),
  ('moderation.reports.gap.seconds','900','Seconds after a report before the same player may send the next.'),
  ('moderation.sanction_ladder.is_on','1','Moderators'' sanctions follow the sanction ladder and players see where they stand on it.'),
  ('moderation.trade_lock.max.hours','168','Longest trade lock a moderator without the ban permission gives, in hours; with it a lock may be permanent.'),
  ('mystery_box.tracker.box_colour','','Colour of the mystery box the tracker shows; empty shows none.'),
  ('mystery_box.tracker.key_colour','','Colour of the mystery key the tracker shows; empty shows none.'),
  ('navigator.event_categories','1,Hottest Events,false;2,Parties & Music,true;3,Role Play,true;4,Help Desk,true;5,Trading,true;6,Games,true;7,Debates & Discussions,true;8,Grand Openings,true;9,Friending,true;10,Jobs,true;11,Group Events,true','Categories of room events: id,name,shown entries separated by semicolons.'),
  ('navigator.newcomer_lobby.room_id','3','Room the client''s newcomer lobby link leads to.'),
  ('navigator.popular.block_style','1','How the popular block is drawn: 0 as rows, 1 as thumbnails.'),
  ('navigator.popular.includes_public_rooms','0','The popular list also shows public rooms.'),
  ('navigator.popular.max_rooms','12','Rooms the popular list shows.'),
  ('navigator.popular.per_category','9','Rooms each category shows on the navigator''s front.'),
  ('navigator.search.max_results','35','Most rooms a navigator search lists per category.'),
  ('navigator.staff_picks.folder_id','1','Public room folder that holds the staff picks.'),
  ('network.accept.threads','1','Threads accepting game connections; 0 takes twice the CPUs.'),
  ('network.bridge.backlog.max','0','Bridge requests of the site that may run or wait at once, more are answered 503; 0 takes 32 for every bridge thread.'),
  ('network.bridge.request.max.bytes','262144','Largest bridge request the site may send.'),
  ('network.bridge.threads','8','Threads for the site''s bridge requests on the game port.'),
  ('network.client_address.header','X-Forwarded-For','Request header a proxy passes the player''s address in; empty reads none.'),
  ('network.client_address.is_proxy_only','0','Only a proxy tells a player''s address: a connection that brought none has no address, instead of the proxy''s own.'),
  ('network.client_address.trusted_proxies','','Addresses of the proxies whose address header is believed, comma separated.'),
  ('network.client_address.trusts_cloudflare','0','The address header is also believed from Cloudflare''s published edge addresses.'),
  ('network.connections.max','20000','Most game sockets open at a time.'),
  ('network.io.threads','0','Threads reading and writing game connections; 0 takes twice the CPUs.'),
  ('network.listen.address','127.0.0.1','Address the game listener binds to; ZEL_LISTEN_HOST sets it at every start, the loopback address behind the game''s own proxy.'),
  ('network.listen.port','2096','Port of the game listener; ZEL_WS_PORT sets it at every start.'),
  ('network.login.timeout.seconds','120','Seconds a new connection has to log in before it closes.'),
  ('network.origin.hosts','','Host names a game connection may come from, comma separated; * is any, empty admits no one. Set from ZEL_SITE_URL and ZEL_EXTRA_ORIGINS at every start.'),
  ('network.output.backlog.max.bytes','8388608','Bytes of unsent output at which a connection closes however fast it reads.'),
  ('network.output.pause.bytes','65536','Bytes of unsent output above which a connection is watched for a stall.'),
  ('network.output.resume.bytes','32768','Bytes of unsent output under which a connection is no longer watched.'),
  ('network.output.stalled.seconds','90','Seconds a watched connection may take no output at all before it closes; a slow one stays.'),
  ('network.packets.backlog.max','0','Packets all connections together may have waiting or running; 0 takes 250 for every packet thread.'),
  ('network.packets.backlog.max.bytes','0','Bytes of packet bodies all connections together may have waiting or running; 0 takes 4 MiB for every packet thread. Over it, a connection holding more than 128 KiB stops reading; one sending small packets reads on.'),
  ('network.packets.backlog.pauses_reading','1','At any packet backlog limit the server stops reading from the connection until its packets are done; off, the backlogs are only counted.'),
  ('network.packets.backlog.per_connection','0','Packets one connection may have waiting or running; 0 takes what it may send in four seconds.'),
  ('network.packets.backlog.per_connection.bytes','0','Bytes of packet bodies one connection may have waiting or running; 0 takes two frames of limits.frame_bytes, never above network.packets.backlog.max.bytes.'),
  ('network.packets.threads','0','Packet handler threads; 0 takes twice the CPUs, 16 at least.'),
  ('network.ping.gap.seconds','60','Seconds a connection may be silent before the server pings it.'),
  ('network.ping.timeout.seconds','120','Seconds a pinged connection has to send anything before it closes.'),
  ('network.upgrade.max.bytes','16384','Largest request that opens a game connection.'),
  ('new_player.welcome_gift.is_offered','0','A new player is offered the welcome gift.'),
  ('pets.name.max.characters','15','Longest name a pet may have.'),
  ('pets.name.min.characters','2','Shortest name a pet may have.'),
  ('pets.respect.account_age.days','0','Days an account must exist before it may respect a pet; 0 is no wait.'),
  ('player.figure.checked_at_football_gate','1','A look is checked against the figure data when a football gate dresses its player.'),
  ('player.figure.checked_at_login','1','A look is checked against the figure data when its player logs in.'),
  ('player.figure.checked_on_change','1','A look is checked against the figure data when its player changes it.'),
  ('player.figure.checked_when_club_ends','1','A look is checked against the figure data when its player''s club membership ends.'),
  ('player.figure.club_items_are_free','1','Club-only clothing and colours are free to wear for every player, as the outfit editor offers them to all; 0 keeps them for club members.'),
  ('player.figure.owned_items_only','0','Clothing a player has to own, such as items bought as clothing, may only be worn by players who own it; 0 lets every player wear all of it.'),
  ('player.figure.data_url','','Where the figure data is read from; empty reads the installed gamedata.'),
  ('player.inventory.max_bots','25','Most bots one inventory holds.'),
  ('player.inventory.max_furni','7500','Most furni one inventory holds.'),
  ('player.inventory.max_pets','25','Most pets one inventory holds.'),
  ('player.motto.max.characters','38','Longest motto a player may set.'),
  ('protocol.unlisted_values.are_dropped','0','A packet with a value outside its list of allowed values is dropped, not only reported.'),
  ('recycler.furni_per_prize','8','Furni the recycler takes for one prize, 1 to 12.'),
  ('recycler.prize.level_1.one_in','1','The recycler draws a level 1 prize once in this many draws; 0 never.'),
  ('recycler.prize.level_2.one_in','4','The recycler draws a level 2 prize once in this many draws; 0 never.'),
  ('recycler.prize.level_3.one_in','10','The recycler draws a level 3 prize once in this many draws; 0 never.'),
  ('recycler.prize.level_4.one_in','40','The recycler draws a level 4 prize once in this many draws; 0 never.'),
  ('recycler.prize.level_5.one_in','400','The recycler draws a level 5 prize once in this many draws; 0 never.'),
  ('recycler.prize.level_6.one_in','5000','The recycler draws a level 6 prize once in this many draws; 0 never.'),
  ('respect.buybacks.per_day','1','Times a day a player may buy their respects back.'),
  ('rollers.units_per_roller','1','Players, bots and pets one roller moves in a pass.'),
  ('room.advertising.badge_code','RADZZ','Badge a player gets for advertising a room; empty for none.'),
  ('room.competition.vote.min_rank','0','Lowest rank that may vote in room competitions; 0 is every player. A player below it gets the client''s "you cannot vote yet" window.'),
  ('room.hosting.is_counted','1','Visitors who stay in a room count towards its owner''s hosting achievement.'),
  ('room.ownerless.owner_name','System','Owner name shown for rooms and furni that have no owner; a content pack names its own.'),
  ('room.public.door_tile_leaves','0','Walking onto the door tile of a public room leaves the room.'),
  ('room.tags.staff_only','staff;official','Room tags only staff may set, separated by semicolons.'),
  ('room.tick.lanes','0','Threads that tick the rooms; 0 sizes them from the CPUs.'),
  ('room.visits.are_logged','1','Every room visit is written to the visit log moderators read.'),
  ('room.walk.drops_any_height','1','A walker steps down further than it steps up.'),
  ('room.walk.max_step_up','1.5','Highest step up a walker takes, in tile heights.'),
  ('self_donation.is_on','0','Players with the debug right may give themselves currency with the self donation tool.'),
  ('self_donation.max_amount','500','Largest amount one self donation gives; the client''s form goes from 1 to 500.'),
  ('server.command_log.max_waiting','100000','Logged command runs that may wait for the database; past it the oldest are let go.'),
  ('server.console.reads_commands','0','The server reads operator commands typed into its console.'),
  ('server.load.refuse.percent','97','Smoothed use of the fullest part (database connections, write queue, packets, scheduler lateness, memory left after collection) from which the load counts as high.'),
  ('server.load.refuses_logins','0','New logins are turned away while the load is high; off, a high load is only logged.'),
  ('server.load.resume.percent','90','Smoothed use every part must be under before the load counts as normal again.'),
  ('server.load.scheduler.late.ms','10000','Wait of the scheduler''s most overdue task that counts as a full scheduler.'),
  ('server.load.smoothing.seconds','10','Time over which the load is smoothed: shorter peaks pass, a lasting load shows after about twice this.'),
  ('server.log.is_debug','0','The server logs at debug level; very verbose, never on a busy game.'),
  ('server.log.shows_packets','0','The server logs every packet it reads and writes.'),
  ('server.scheduler.threads','0','Threads of the shared scheduler for timed and short tasks; 0 takes one per processor, four at least.'),
  ('server.stop.write_drain.seconds','90','Longest the stop waits for queued database writes, 150 at most.'),
  ('server.version','','The version that last started on this database; the server writes it.'),
  ('site.registration.accounts_per_address','5','Accounts one address may register; 0 is no cap. Local addresses are not counted.'),
  ('site.registration.home_room_id','0','Room a new player has as home room; 0 for none.'),
  ('site.registration.motto','','Motto a new player starts with.'),
  ('snowball.arenas','8,9,11','Arenas games are played in, comma separated.'),
  ('snowball.arenas.folder','','Folder with the Snowball arenas of the content pack; set from ZEL_DATA_DIR at every start.'),
  ('snowball.assets.url','','Address the client loads the Snowball images from; empty: the folder the content pack names.'),
  ('snowball.loading.timeout.seconds','30','Seconds a client has to load a game before the game starts without it.'),
  ('snowball.lobby.countdown.seconds','15','Seconds a full lobby counts down before its game starts.'),
  ('snowball.max_games','20','Games that may run at once.'),
  ('snowball.min_players','2','Players a game needs to start.'),
  ('snowball.rejoin_block.seconds','120','Seconds a player who left a running game waits before joining another.'),
  ('snowball.rematch.window.seconds','30','Seconds after a game in which its players may ask for a rematch.'),
  ('snowball.stage.duration.seconds','120','Seconds one game lasts.'),
  ('snowball.weekly.group_prize.points','50,25,15','Points for the first, second and third group of the week, comma separated.'),
  ('snowball.weekly.lucky.prize','','Furni a lucky winner gets; a content pack names it, empty gives none.'),
  ('snowball.weekly.lucky.ranks','4-50','Ranks of the week the lucky winners are drawn from, as from-to.'),
  ('snowball.weekly.lucky.winners','3','Lucky winners drawn each week.'),
  ('snowball.weekly.rank_prizes','','Furni for the first, second and third player of the week, comma separated; a content pack names them, empty gives none.'),
  ('tileclaim.points.per_filled_tile','0','Points for each tile an enclosed area fills.'),
  ('tileclaim.points.per_locked_tile','1','Points for locking a tile.'),
  ('tileclaim.points.per_taken_tile','0','Points for taking a tile another team holds.'),
  ('trading.credit_furni.is_counted','0','The trade window counts the credits of the credit furni in each offer.'),
  ('trading.trades.are_logged','1','Every completed trade is written to the trade log.'),
  ('treasure_hunt.is_on','1','A click on a furni that is a clue in treasure_hunt_clues counts as a find.'),
  ('wired.bot_line.max.characters','100','Longest line a wired box makes a bot say.'),
  ('wired.bot_name.max.characters','100','Longest bot name a wired box takes.'),
  ('wired.box.max_delay.half_seconds','20','Longest delay a wired box takes, in half seconds.'),
  ('wired.box.max_picked_furni','20','Furni one wired box may pick.'),
  ('wired.chest.starter.description','Your very first chest! Every great collector starts somewhere','Description of the chest a player is given first.'),
  ('wired.chest.starter.name','Starter chest','Name of the chest a player is given first.'),
  ('wired.links.networks_per_player','10000','Link networks one player may hold.'),
  ('wired.monitor.heavy_after.seconds','10','Seconds of dropped firings after which a room is marked heavy; calm time takes the mark off again.'),
  ('wired.monitor.max_waiting_runs','2000','Delayed runs a room may hold waiting; past it a firing loses its delays.'),
  ('wired.monitor.pause.seconds','60','Length of a room''s first wired pause; each further pause within the hour is twice as long, up to sixteen times.'),
  ('wired.monitor.pause_after.seconds','0','Seconds of dropped firings after which a room''s wired is paused; 0 never pauses.'),
  ('wired.monitor.run_burst.ms','500','Run time a room''s wired may be ahead of its share before its firings are dropped; also the longest one run may go on starting new work.'),
  ('wired.monitor.run_share.percent','10','Share of one processor core a room''s wired may use over time.'),
  ('wired.monitor.usage_allowance','0','Wired usage allowance of every room, whatever its owner''s level; 0 follows the owner''s account level.'),
  ('wired.show_message.max.characters','400','Longest message a show message box keeps; a longer one is cut.'),
  ('wired.show_message.max.lines','8','Most lines a show message box keeps.'),
  ('wired.teleport.duration.ms','2000','Milliseconds from a wired teleport''s effect to its landing, on the 0.5 s room tick; the fast option keeps its own 1 s.'),
  ('word_filter.covers_messenger','1','The word filter reads messenger messages.'),
  ('word_filter.covers_rooms','1','The word filter reads room chat.'),
  ('word_filter.is_on','1','The word filter is on; off, nothing is filtered anywhere.'),
  ('word_filter.normalises_text','1','The word filter reads accented and styled letters and look-alike characters (0 for o, 1 or ! for i or l) as the plain letter before it compares.');

-- texts. Texts the server sends in its own words.
INSERT INTO `texts` (`name`, `value`) VALUES
  ('catalog.gifts.receiver_full','That player''s inventory is full, so the gift can''t be delivered. Nothing was charged.'),
  ('catalog.limited.daily_cap.all','You''ve already bought %limit% limited items today, and that''s the daily maximum. Come back tomorrow for more.'),
  ('catalog.limited.daily_cap.item','You''ve already bought %limit% of the limited %item% today, and that''s the daily maximum. Come back tomorrow for more.'),
  ('catalog.voucher.locked','Too many wrong voucher codes. You can try again in %minutes% minutes.'),
  ('club.payday.paid','It''s club payday! %amount% has been paid into your purse.'),
  ('command.mutebots.off','You can see bot chat again.'),
  ('command.mutebots.on','Bot chat is hidden for you now. Type :mutebots again to bring it back.'),
  ('command.mutepets.off','You can see pet chat again.'),
  ('command.mutepets.on','Pet chat is hidden for you now. Type :mutepets again to bring it back.'),
  ('inventory.bots.full','You can keep %limit% bots in your inventory at most. Put one in a room to make space.'),
  ('inventory.furni.full','Your inventory is full. Put some furni in a room or trade it away before you get more.'),
  ('inventory.pets.full','You can keep %limit% pets in your inventory at most. Put one in a room to make space.'),
  ('moderation.mute.done','%player% is muted now.'),
  ('moderation.player.unknown','We couldn''t find that player.'),
  ('moderation.room.cleared_name','Name removed by a moderator'),
  ('moderation.ticket.not_yours','Another moderator is working on this ticket, so you can''t close it.'),
  ('moderation.ticket.pick_failed','You couldn''t take that ticket.<br>Someone else has it already, or it has been closed.'),
  ('room.bot.butler.served','There you go, %player%: one %item%.'),
  ('room.bot.visitor.entry','%player% at %time%'),
  ('room.bot.visitor.nobody','Nobody came by while you were out.'),
  ('room.bot.visitor.report','Your last %count% visitors:%visits%'),
  ('room.bot.visitor.summary','%count% visitors came by while you were out. Say %word% and I''ll tell you who.'),
  ('room.bot.visitor.word','yes'),
  ('room.floor_plan.not_allowed','You''re not allowed to change the floor plan of this room.'),
  ('room.furni.ad.not_allowed','You''re not allowed to edit advertisement furni.'),
  ('room.furni.sticky_pole.note_header','%time%, %player%:'),
  ('room.furni.talking.pirate_parrot.0','Squawk!'),
  ('room.furni.talking.pirate_parrot.1','Who''s a pretty bird? I am!'),
  ('room.furni.talking.pirate_parrot.2','Any crackers left for me?'),
  ('room.furni.talking.pirate_parrot.3','Land ahoy! Or was that a cloud?'),
  ('room.promotion.text_refused','That text has words in it that we don''t allow. Please reword it and try again.'),
  ('tour.step.1','This button takes you back to the welcome screen whenever you like.'),
  ('tour.step.10','Your friends show up down here once you have made some.'),
  ('tour.step.11','That''s the tour. Pick a room and go meet people!'),
  ('tour.step.2','The navigator lists the rooms you can visit. Open it to go somewhere new.'),
  ('tour.step.3','Type here and press Enter to say something to the room.'),
  ('tour.step.4','Missed what someone said? The chat history lets you scroll back.'),
  ('tour.step.5','Fancy a new outfit? Change your clothes and colours here.'),
  ('tour.step.6','The shop sells furni for decorating a room of your own.'),
  ('tour.step.7','You pay for furni with credits,'),
  ('tour.step.8','with points,'),
  ('tour.step.9','or with premium currency.'),
  ('wired.kick.spared.owner','Wired kick exception: Room owner'),
  ('wired.kick.spared.staff','Wired tried to kick you, but staff stay put.');

-- ranks
INSERT INTO `ranks` (`id`, `name`, `badge_code`, `log_commands`, `max_rooms`, `max_friends`, `max_favourite_rooms`) VALUES
  (0,'Member','',0,0,0,0),
  (1,'Member (no ads)','',0,0,0,0),
  (2,'VIP','',0,0,0,0),
  (3,'Ambassador','AMB',0,0,0,0),
  (4,'Employee','ADM',0,0,0,0),
  (5,'Moderator','ADM',0,0,0,0),
  (6,'Player support','ADM',0,0,0,0),
  (7,'Community','ADM',0,0,0,0),
  (8,'Administrator','ADM',0,0,0,0),
  (9,'Super user','ADM',0,0,0,0);

-- permissions
INSERT INTO `permissions` (`code`, `description`) VALUES
  ('ambassador.tools','Ambassador powers in public rooms: kick, alert, mute for 2 minutes up to 72 hours and unmute from the avatar menu; also the ambassador chat bubble.'),
  ('bots.unlimited','No cap on bots, in the inventory or in a room.'),
  ('calendar.force_open','Open any day of a campaign calendar with its Force open button.'),
  ('chat.any_bubble','Pick every chat bubble, the staff ones included.'),
  ('chat.mute_immune','Is never muted, whoever tries.'),
  ('chat.no_flood_limit','Chat is not held back by the flood limit.'),
  ('chat.unfiltered','Own chat and the lines of own bots pass the word filter untouched.'),
  ('command.kick',':kick <name> sends a lower-ranked player out of the room you are in.'),
  ('command.mute',':mute <name> (or :shutup) silences a lower-ranked player in the room you are in for 2 minutes.'),
  ('debug.self_donation','The client\'s :donate tool, and only while self donation is switched on in the settings.'),
  ('friends.follow_any','Follow a friend to their room even when they switched following off.'),
  ('friends.unlimited','No cap on the friend list.'),
  ('group.manage_any','Act as admin of every group: settings, members, badge, colours and removal.'),
  ('group.pass_any_gate','Walk through the gate of every group.'),
  ('housekeeping.badges','Housekeeping: give a player a badge or take one away.'),
  ('housekeeping.landing_view','Housekeeping: write, order and remove the articles of the landing view.'),
  ('housekeeping.news','Housekeeping: write, order, show, hide and remove the news articles of the site.'),
  ('housekeeping.ranks','Housekeeping: move a player to another level.'),
  ('housekeeping.settings','Housekeeping: make the running game read its settings again.'),
  ('housekeeping.wallet','Housekeeping: set the amounts in a player\'s wallet.'),
  ('housekeeping.youtube','Housekeeping: the YouTube playlists of the video furni and the YouTube key.'),
  ('icons.own_all','Holds every collectable icon without earning it.'),
  ('moderation.player.alert','Send a player an alert or a caution.'),
  ('moderation.player.ban','Ban and unban players and devices; in housekeeping also the identity page and the audit log.'),
  ('moderation.player.chatlog','Read what a player said and which rooms they visited.'),
  ('moderation.player.kick','Send a player out of the room they are in.'),
  ('moderation.player.mute','Mute a player from the moderation tool, online or not.'),
  ('moderation.player.trade_lock','Bar a player from trading for a while.'),
  ('moderation.room.alert','Send a message to everyone in a room; from housekeeping, ask a room a question.'),
  ('moderation.room.chatlog','Read what was said in a room.'),
  ('moderation.room.info','Look up a room: its details and who is in it.'),
  ('moderation.room.intervene','Step into a room from the moderation tool: send everyone out, lock its door, replace its name.'),
  ('moderation.tickets','Work the help tickets: pick, release and close them, and answer help requests.'),
  ('moderation.tool','Open the moderation tool and housekeeping; every action in them asks for its own permission.'),
  ('navigator.staff_picks','Put a room among the staff picks or take it out.'),
  ('perk.builder_at_work','Client feature BUILDER_AT_WORK.'),
  ('perk.camera','Client feature CAMERA: the camera in a room.'),
  ('perk.citizen','Client feature CITIZEN.'),
  ('perk.club_offer','Client feature of the club offer.'),
  ('perk.mouse_zoom','Client feature MOUSE_ZOOM.'),
  ('perk.new_navigator','Client feature NAVIGATOR_PHASE_TWO_2014: the current navigator.'),
  ('perk.room_thumbnail_camera','Client feature NAVIGATOR_ROOM_THUMBNAIL_CAMERA.'),
  ('perk.vote_in_competitions','Client feature VOTE_IN_COMPETITIONS; held from the rank of the setting room.competition.vote.min_rank on, never granted per rank.'),
  ('pets.unlimited','No cap on pets, in the inventory or in a room.'),
  ('room.advertisements','Set the picture and link of advertisement furni.'),
  ('room.enter_any','Walk into every room, whatever its doorbell, password or ban list says.'),
  ('room.enter_full','Walk into a room that is full.'),
  ('room.floor_plan','Save a floor plan where the editor is kept for staff: in own rooms, and in every room together with room.manage_any.'),
  ('room.kick_immune','Cannot be sent out or muted by room owners, wired or the cannon.'),
  ('room.manage_any','Owner rights in every room: settings, rights, wired, floor plan, pick up, kick and ban.'),
  ('room.move_anywhere','Move and turn furni in every room.'),
  ('room.place_anywhere','Place furni, pets and bots in every room, as someone with rights there.'),
  ('room.trade_anywhere','Trade in rooms that have trading switched off.'),
  ('wired.staff','Wired kept for staff: the reward effect, and wired texts that skip the word filter.');

-- rank_permissions
INSERT INTO `rank_permissions` (`rank_id`, `permission_code`) VALUES
  (0,'perk.camera'),
  (0,'perk.citizen'),
  (0,'perk.club_offer'),
  (0,'perk.mouse_zoom'),
  (0,'perk.new_navigator'),
  (0,'perk.room_thumbnail_camera'),
  (1,'perk.camera'),
  (1,'perk.citizen'),
  (1,'perk.club_offer'),
  (1,'perk.mouse_zoom'),
  (1,'perk.new_navigator'),
  (1,'perk.room_thumbnail_camera'),
  (2,'perk.camera'),
  (2,'perk.citizen'),
  (2,'perk.club_offer'),
  (2,'perk.mouse_zoom'),
  (2,'perk.new_navigator'),
  (2,'perk.room_thumbnail_camera'),
  (2,'room.enter_full'),
  (3,'ambassador.tools'),
  (3,'perk.camera'),
  (3,'perk.citizen'),
  (3,'perk.club_offer'),
  (3,'perk.mouse_zoom'),
  (3,'perk.new_navigator'),
  (3,'perk.room_thumbnail_camera'),
  (3,'room.enter_full'),
  (4,'ambassador.tools'),
  (4,'chat.any_bubble'),
  (4,'chat.no_flood_limit'),
  (4,'command.kick'),
  (4,'command.mute'),
  (4,'perk.builder_at_work'),
  (4,'perk.camera'),
  (4,'perk.citizen'),
  (4,'perk.club_offer'),
  (4,'perk.mouse_zoom'),
  (4,'perk.new_navigator'),
  (4,'perk.room_thumbnail_camera'),
  (4,'room.enter_full'),
  (5,'ambassador.tools'),
  (5,'calendar.force_open'),
  (5,'chat.any_bubble'),
  (5,'chat.mute_immune'),
  (5,'chat.no_flood_limit'),
  (5,'command.kick'),
  (5,'command.mute'),
  (5,'friends.follow_any'),
  (5,'group.pass_any_gate'),
  (5,'moderation.player.alert'),
  (5,'moderation.player.ban'),
  (5,'moderation.player.chatlog'),
  (5,'moderation.player.kick'),
  (5,'moderation.player.mute'),
  (5,'moderation.player.trade_lock'),
  (5,'moderation.room.alert'),
  (5,'moderation.room.chatlog'),
  (5,'moderation.room.info'),
  (5,'moderation.room.intervene'),
  (5,'moderation.tickets'),
  (5,'moderation.tool'),
  (5,'perk.builder_at_work'),
  (5,'perk.camera'),
  (5,'perk.citizen'),
  (5,'perk.club_offer'),
  (5,'perk.mouse_zoom'),
  (5,'perk.new_navigator'),
  (5,'perk.room_thumbnail_camera'),
  (5,'room.enter_any'),
  (5,'room.enter_full'),
  (5,'room.kick_immune'),
  (5,'room.manage_any'),
  (5,'room.place_anywhere'),
  (6,'ambassador.tools'),
  (6,'calendar.force_open'),
  (6,'chat.any_bubble'),
  (6,'chat.mute_immune'),
  (6,'chat.no_flood_limit'),
  (6,'command.kick'),
  (6,'command.mute'),
  (6,'friends.follow_any'),
  (6,'group.pass_any_gate'),
  (6,'moderation.player.alert'),
  (6,'moderation.player.ban'),
  (6,'moderation.player.chatlog'),
  (6,'moderation.player.kick'),
  (6,'moderation.player.mute'),
  (6,'moderation.player.trade_lock'),
  (6,'moderation.room.alert'),
  (6,'moderation.room.chatlog'),
  (6,'moderation.room.info'),
  (6,'moderation.room.intervene'),
  (6,'moderation.tickets'),
  (6,'moderation.tool'),
  (6,'perk.builder_at_work'),
  (6,'perk.camera'),
  (6,'perk.citizen'),
  (6,'perk.club_offer'),
  (6,'perk.mouse_zoom'),
  (6,'perk.new_navigator'),
  (6,'perk.room_thumbnail_camera'),
  (6,'room.enter_any'),
  (6,'room.enter_full'),
  (6,'room.kick_immune'),
  (6,'room.manage_any'),
  (6,'room.place_anywhere'),
  (7,'ambassador.tools'),
  (7,'calendar.force_open'),
  (7,'chat.any_bubble'),
  (7,'chat.mute_immune'),
  (7,'chat.no_flood_limit'),
  (7,'command.kick'),
  (7,'command.mute'),
  (7,'friends.follow_any'),
  (7,'group.pass_any_gate'),
  (7,'housekeeping.landing_view'),
  (7,'housekeeping.news'),
  (7,'moderation.player.alert'),
  (7,'moderation.player.ban'),
  (7,'moderation.player.chatlog'),
  (7,'moderation.player.kick'),
  (7,'moderation.player.mute'),
  (7,'moderation.player.trade_lock'),
  (7,'moderation.room.alert'),
  (7,'moderation.room.chatlog'),
  (7,'moderation.room.info'),
  (7,'moderation.room.intervene'),
  (7,'moderation.tickets'),
  (7,'moderation.tool'),
  (7,'navigator.staff_picks'),
  (7,'perk.builder_at_work'),
  (7,'perk.camera'),
  (7,'perk.citizen'),
  (7,'perk.club_offer'),
  (7,'perk.mouse_zoom'),
  (7,'perk.new_navigator'),
  (7,'perk.room_thumbnail_camera'),
  (7,'room.enter_any'),
  (7,'room.enter_full'),
  (7,'room.kick_immune'),
  (7,'room.manage_any'),
  (7,'room.place_anywhere'),
  (8,'ambassador.tools'),
  (8,'bots.unlimited'),
  (8,'calendar.force_open'),
  (8,'chat.any_bubble'),
  (8,'chat.mute_immune'),
  (8,'chat.no_flood_limit'),
  (8,'chat.unfiltered'),
  (8,'command.kick'),
  (8,'command.mute'),
  (8,'friends.follow_any'),
  (8,'friends.unlimited'),
  (8,'group.manage_any'),
  (8,'group.pass_any_gate'),
  (8,'housekeeping.badges'),
  (8,'housekeeping.landing_view'),
  (8,'housekeeping.news'),
  (8,'housekeeping.wallet'),
  (8,'housekeeping.youtube'),
  (8,'icons.own_all'),
  (8,'moderation.player.alert'),
  (8,'moderation.player.ban'),
  (8,'moderation.player.chatlog'),
  (8,'moderation.player.kick'),
  (8,'moderation.player.mute'),
  (8,'moderation.player.trade_lock'),
  (8,'moderation.room.alert'),
  (8,'moderation.room.chatlog'),
  (8,'moderation.room.info'),
  (8,'moderation.room.intervene'),
  (8,'moderation.tickets'),
  (8,'moderation.tool'),
  (8,'navigator.staff_picks'),
  (8,'perk.builder_at_work'),
  (8,'perk.camera'),
  (8,'perk.citizen'),
  (8,'perk.club_offer'),
  (8,'perk.mouse_zoom'),
  (8,'perk.new_navigator'),
  (8,'perk.room_thumbnail_camera'),
  (8,'pets.unlimited'),
  (8,'room.advertisements'),
  (8,'room.enter_any'),
  (8,'room.enter_full'),
  (8,'room.floor_plan'),
  (8,'room.kick_immune'),
  (8,'room.manage_any'),
  (8,'room.move_anywhere'),
  (8,'room.place_anywhere'),
  (8,'room.trade_anywhere'),
  (8,'wired.staff'),
  (9,'ambassador.tools'),
  (9,'bots.unlimited'),
  (9,'calendar.force_open'),
  (9,'chat.any_bubble'),
  (9,'chat.mute_immune'),
  (9,'chat.no_flood_limit'),
  (9,'chat.unfiltered'),
  (9,'command.kick'),
  (9,'command.mute'),
  (9,'debug.self_donation'),
  (9,'friends.follow_any'),
  (9,'friends.unlimited'),
  (9,'group.manage_any'),
  (9,'group.pass_any_gate'),
  (9,'housekeeping.badges'),
  (9,'housekeeping.landing_view'),
  (9,'housekeeping.news'),
  (9,'housekeeping.ranks'),
  (9,'housekeeping.settings'),
  (9,'housekeeping.wallet'),
  (9,'housekeeping.youtube'),
  (9,'icons.own_all'),
  (9,'moderation.player.alert'),
  (9,'moderation.player.ban'),
  (9,'moderation.player.chatlog'),
  (9,'moderation.player.kick'),
  (9,'moderation.player.mute'),
  (9,'moderation.player.trade_lock'),
  (9,'moderation.room.alert'),
  (9,'moderation.room.chatlog'),
  (9,'moderation.room.info'),
  (9,'moderation.room.intervene'),
  (9,'moderation.tickets'),
  (9,'moderation.tool'),
  (9,'navigator.staff_picks'),
  (9,'perk.builder_at_work'),
  (9,'perk.camera'),
  (9,'perk.citizen'),
  (9,'perk.club_offer'),
  (9,'perk.mouse_zoom'),
  (9,'perk.new_navigator'),
  (9,'perk.room_thumbnail_camera'),
  (9,'pets.unlimited'),
  (9,'room.advertisements'),
  (9,'room.enter_any'),
  (9,'room.enter_full'),
  (9,'room.floor_plan'),
  (9,'room.kick_immune'),
  (9,'room.manage_any'),
  (9,'room.move_anywhere'),
  (9,'room.place_anywhere'),
  (9,'room.trade_anywhere'),
  (9,'wired.staff');

-- sanction_steps. The sanction ladder: hours of the sanction, days until it counts no more.
INSERT INTO `sanction_steps` (`step`, `kind`, `hours`, `probation_days`) VALUES
  (1,'ALERT',0,30),
  (2,'MUTE',1,30),
  (3,'BAN',18,30),
  (4,'BAN',168,30),
  (5,'BAN',720,60),
  (6,'BAN',720,60),
  (7,'BAN',876581,876581);

-- rooms
INSERT INTO `room_models` (`name`, `door_x`, `door_y`, `door_direction`, `heightmap`) VALUES
  ('model_a',3,5,2,'xxxxxxxxxxxx\nxxxx00000000\nxxxx00000000\nxxxx00000000\nxxxx00000000\nxxx000000000\nxxxx00000000\nxxxx00000000\nxxxx00000000\nxxxx00000000\nxxxx00000000\nxxxx00000000\nxxxx00000000\nxxxx00000000\nxxxxxxxxxxxx\nxxxxxxxxxxxx');
INSERT INTO `room_categories` (`id`, `min_rank`, `caption_key`, `caption`, `name`, `visible`, `automatic`, `automatic_key`, `staff_only`, `max_players`, `is_public`, `list_mode`, `sort_order`) VALUES
  (1,0,'caption_save_general','General','general',1,0,'NONE',0,100,0,0,0);
