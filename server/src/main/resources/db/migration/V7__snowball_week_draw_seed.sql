ALTER TABLE `snowball_weeks`
  ADD COLUMN `draw_seed` bigint(20) NOT NULL DEFAULT 0 AFTER `processed_at`,
  COMMENT = 'Weeks whose rewards were paid. draw_seed is the secret that seeds the weekly lucky draw.';
