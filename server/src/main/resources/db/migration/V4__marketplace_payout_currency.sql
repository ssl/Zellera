ALTER TABLE `marketplace_listings`
  ADD COLUMN `payout_currency` int(11) DEFAULT NULL AFTER `state`,
  COMMENT = 'Furni on the marketplace. While listed, the furni''s owner is NULL. payout_currency is stamped at sale.';
