ALTER TABLE `player_accounts`
  ADD COLUMN `login_ticket_ip` varchar(45) DEFAULT NULL AFTER `login_ticket_device`;

CREATE TABLE `suspicions` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `player_id` int(11) DEFAULT NULL,
  `address` varchar(45) NOT NULL DEFAULT '',
  `device` varchar(120) NOT NULL DEFAULT '',
  `kind` varchar(40) NOT NULL,
  `weight` tinyint(3) unsigned NOT NULL DEFAULT 1,
  `detail` varchar(255) NOT NULL DEFAULT '',
  `created_at` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_suspicions_1` (`player_id`, `created_at`),
  KEY `ix_suspicions_2` (`created_at`),
  KEY `ix_suspicions_3` (`kind`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
