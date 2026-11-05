-- V5: 题目可见性
-- PUBLIC  : 官方公共题，计入段位 rank_ac
-- SHARED  : 用户共享题，所有人可练习，但 AC 不计入段位
-- PRIVATE : 用户私有题，仅作者可见
ALTER TABLE question
  ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'PRIVATE'
    COMMENT 'PUBLIC/PRIVATE/SHARED' AFTER type;

UPDATE question SET visibility = 'PUBLIC' WHERE type = 'PUBLIC';
UPDATE question SET visibility = 'PRIVATE' WHERE type = 'PERSONAL';

CREATE INDEX idx_type_visibility ON question (type, visibility, user_id);
