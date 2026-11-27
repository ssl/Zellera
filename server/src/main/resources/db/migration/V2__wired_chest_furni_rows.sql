ALTER TABLE `wired_chest_furni`
  ADD COLUMN `furni_id` int(11) DEFAULT NULL AFTER `extra`,
  ADD KEY `fk_wired_chest_furni_3` (`furni_id`),
  ADD CONSTRAINT `fk_wired_chest_furni_3` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE SET NULL,
  COMMENT = 'Furni inside a chest. furni_id is its furni row, kept without an owner while it is inside.';
