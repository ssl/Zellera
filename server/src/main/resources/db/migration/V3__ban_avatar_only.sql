ALTER TABLE `bans`
  ADD COLUMN `avatar_only` tinyint(1) NOT NULL DEFAULT 0 AFTER `barred_machine`,
  COMMENT = 'Bans, each on one player until its end. avatar_only bans the avatar alone and never counts for the device rules.';
