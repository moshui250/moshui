-- CodeAgent 竞赛模块（手工执行，MySQL 8）
-- 幂等：表使用 IF NOT EXISTS；submit_record.contest_id 与索引先检查再 ALTER。
USE codeagent;
SET NAMES utf8mb4;

-- 1. 给提交记录增加竞赛归属；普通练习为 NULL。
SET @has_contest_col = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'codeagent' AND TABLE_NAME = 'submit_record' AND COLUMN_NAME = 'contest_id'
);
SET @ddl = IF(@has_contest_col = 0,
  'ALTER TABLE submit_record ADD COLUMN contest_id BIGINT UNSIGNED NULL COMMENT ''所属竞赛；普通练习为 NULL'' AFTER question_id',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = 'codeagent' AND TABLE_NAME = 'submit_record' AND INDEX_NAME = 'idx_contest_user'
);
SET @ddl = IF(@has_idx = 0,
  'ALTER TABLE submit_record ADD KEY idx_contest_user (contest_id, user_id, create_time)',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = 'codeagent' AND TABLE_NAME = 'submit_record' AND INDEX_NAME = 'idx_contest_q'
);
SET @ddl = IF(@has_idx = 0,
  'ALTER TABLE submit_record ADD KEY idx_contest_q (contest_id, question_id, status)',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 竞赛
CREATE TABLE IF NOT EXISTS contest (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  title       VARCHAR(128) NOT NULL,
  description TEXT,
  start_time  DATETIME NOT NULL,
  end_time    DATETIME NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_contest_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞赛';

-- 3. 竞赛题单
CREATE TABLE IF NOT EXISTS contest_problem (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  contest_id  BIGINT UNSIGNED NOT NULL,
  question_id BIGINT UNSIGNED NOT NULL,
  label       VARCHAR(8) NOT NULL COMMENT 'A/B/C/D',
  sort_no     INT NOT NULL DEFAULT 0,
  score       INT NOT NULL DEFAULT 100,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_contest_question (contest_id, question_id),
  UNIQUE KEY uk_contest_label (contest_id, label),
  KEY idx_contest_sort (contest_id, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞赛题单';

-- 4. 竞赛报名
CREATE TABLE IF NOT EXISTS contest_registration (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  contest_id  BIGINT UNSIGNED NOT NULL,
  user_id     BIGINT UNSIGNED NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_contest_user (contest_id, user_id),
  KEY idx_contest_reg (contest_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞赛报名';

-- 5. 演示竞赛：1 进行中，2 未开始，3 已结束。
INSERT INTO contest (id, title, description, start_time, end_time, create_time, update_time, deleted) VALUES
(1, 'CodeAgent 周赛 #1', '四道基础题，覆盖数组、字符串、排序与链表。比赛期间提交会计入竞赛排名。',
 DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR), NOW(), NOW(), 0),
(2, '算法入门挑战', '面向新手的数组与递归入门赛，提前报名，开始后统一计时。',
 DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY), NOW(), NOW(), 0),
(3, '链表专项赛', '链表反转、回文与重排，已结束，可查看题单与排名。',
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  title = VALUES(title), description = VALUES(description),
  start_time = VALUES(start_time), end_time = VALUES(end_time), deleted = 0;

INSERT INTO contest_problem (id, contest_id, question_id, label, sort_no, score) VALUES
(1, 1, 2,  'A', 1, 100),
(2, 1, 4,  'B', 2, 100),
(3, 1, 13, 'C', 3, 100),
(4, 1, 7,  'D', 4, 100),
(5, 2, 1,  'A', 1, 100),
(6, 2, 5,  'B', 2, 100),
(7, 2, 10, 'C', 3, 100),
(8, 3, 8,  'A', 1, 100),
(9, 3, 9,  'B', 2, 100),
(10, 3, 11, 'C', 3, 100)
ON DUPLICATE KEY UPDATE
  contest_id = VALUES(contest_id), question_id = VALUES(question_id),
  label = VALUES(label), sort_no = VALUES(sort_no), score = VALUES(score);

-- demo 用户报名进行中的周赛；其他账号可自行报名。
INSERT INTO contest_registration (contest_id, user_id, create_time)
SELECT 1, id, NOW() FROM sys_user WHERE username = 'demo' LIMIT 1
ON DUPLICATE KEY UPDATE create_time = VALUES(create_time);

SELECT
  (SELECT COUNT(*) FROM contest) AS contests,
  (SELECT COUNT(*) FROM contest_problem) AS contest_problems,
  (SELECT COUNT(*) FROM contest_registration) AS registrations;
