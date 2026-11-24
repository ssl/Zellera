CREATE TABLE ext_guestbook_entries (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_id INT NOT NULL,
  author_id INT NOT NULL,
  message VARCHAR(200) NOT NULL,
  created_at INT UNSIGNED NOT NULL,
  PRIMARY KEY (id),
  KEY ix_owner (owner_id, id),
  KEY ix_author (author_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Messages players leave on a profile; owner and author are player ids.';
