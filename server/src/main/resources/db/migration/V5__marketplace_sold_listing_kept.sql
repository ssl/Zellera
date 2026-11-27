ALTER TABLE `marketplace_listings`
  DROP FOREIGN KEY `fk_marketplace_listings_1`,
  MODIFY COLUMN `furni_id` int(11) DEFAULT NULL,
  ADD CONSTRAINT `fk_marketplace_listings_1` FOREIGN KEY (`furni_id`) REFERENCES `furni` (`id`) ON DELETE SET NULL;
