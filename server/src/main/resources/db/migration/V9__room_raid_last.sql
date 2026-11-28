ALTER TABLE `room_raid_settings`
  ADD COLUMN `last_raid_at` int(10) unsigned NOT NULL DEFAULT 0 AFTER `guard_sensitivity`;
