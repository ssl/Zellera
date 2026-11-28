DELETE FROM `settings` WHERE `name` IN ('maintenance.is_active', 'maintenance.duration.minutes');
UPDATE `settings` SET `description` = 'Lowest rank that still gets in after closing time.'
  WHERE `name` = 'maintenance.lowest_rank_in';
