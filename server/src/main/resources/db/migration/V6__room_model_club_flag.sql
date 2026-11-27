ALTER TABLE `room_models`
  ADD COLUMN `club_only` tinyint(1) NOT NULL DEFAULT 0 AFTER `heightmap`;
