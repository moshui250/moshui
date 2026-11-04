-- CodeAgent 智码刷题平台 · 初始化表结构（MySQL 8）
-- 手工执行，不接入 Flyway，避免启动时误改线上数据。
CREATE DATABASE IF NOT EXISTS codeagent
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE codeagent;

SET NAMES utf8mb4;

-- 1. 用户
CREATE TABLE IF NOT EXISTS sys_user (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username    VARCHAR(32)  NOT NULL COMMENT '登录名，4-20位',
  password    VARCHAR(72)  NOT NULL COMMENT 'BCrypt 密文',
  nickname    VARCHAR(32)  NOT NULL DEFAULT '',
  role        VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT 'USER/ADMIN',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '保留不用：用户不做逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- 2. 知识点
CREATE TABLE IF NOT EXISTS knowledge_point (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code        VARCHAR(32)  NOT NULL COMMENT 'array/string/linked_list/recursion/sort',
  name        VARCHAR(32)  NOT NULL,
  description TEXT,
  sort        INT          NOT NULL DEFAULT 0,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点';

-- 3. 题目
CREATE TABLE IF NOT EXISTS question (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  title               VARCHAR(128) NOT NULL,
  description         TEXT         NOT NULL COMMENT 'Markdown 题面',
  input_desc          TEXT,
  output_desc         TEXT,
  difficulty          TINYINT      NOT NULL DEFAULT 1 COMMENT '1易 2中 3难',
  knowledge_point_id  BIGINT UNSIGNED NOT NULL,
  template_json       JSON         COMMENT '{"cpp":"...","java":"..."}',
  type                VARCHAR(16)  NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC/PERSONAL',
  user_id             BIGINT UNSIGNED NULL COMMENT '公共题为 NULL',
  source_image_url    VARCHAR(255) NULL,
  create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted             TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_kp_diff (knowledge_point_id, difficulty),
  KEY idx_type_user (type, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目';

-- 4. 测试用例（样例也是用例：is_sample=1）
CREATE TABLE IF NOT EXISTS question_test_case (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  question_id     BIGINT UNSIGNED NOT NULL,
  input_data      TEXT    NOT NULL,
  expected_output TEXT    NOT NULL,
  is_sample       TINYINT NOT NULL DEFAULT 0 COMMENT '1=可下发前端',
  score           INT     NOT NULL DEFAULT 25,
  sort_no         INT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_question (question_id, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用例';

-- 5. 参考题解（第二层提示 + RAG 来源）
CREATE TABLE IF NOT EXISTS question_solution (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  question_id BIGINT UNSIGNED NOT NULL,
  language    VARCHAR(16) NOT NULL COMMENT 'CPP/JAVA',
  content     TEXT NOT NULL COMMENT '可直接运行的完整代码',
  explanation TEXT COMMENT '思路讲解',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_q_lang (question_id, language)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参考题解';

-- 6. 提交记录
CREATE TABLE IF NOT EXISTS submit_record (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id          BIGINT UNSIGNED NOT NULL,
  question_id      BIGINT UNSIGNED NOT NULL,
  language         VARCHAR(16) NOT NULL,
  code             MEDIUMTEXT  NOT NULL,
  status           VARCHAR(16) NOT NULL COMMENT 'PENDING/JUDGING/AC/WA/TLE/MLE/RE/CE/SE',
  passed_count     INT NOT NULL DEFAULT 0,
  total_count      INT NOT NULL DEFAULT 0,
  run_time_ms      INT NOT NULL DEFAULT 0 COMMENT '最大单用例耗时',
  compile_output   TEXT,
  error_message    TEXT,
  case_result_json JSON,
  create_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_q_time (user_id, question_id, create_time),
  KEY idx_q_status (question_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提交记录';

-- 7. 错题本
CREATE TABLE IF NOT EXISTS user_wrong_question (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id          BIGINT UNSIGNED NOT NULL,
  question_id      BIGINT UNSIGNED NOT NULL,
  wrong_count      INT NOT NULL DEFAULT 1,
  last_submit_id   BIGINT UNSIGNED NULL,
  mastered         TINYINT NOT NULL DEFAULT 0,
  mastered_time    DATETIME NULL,
  first_wrong_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_wrong_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_q (user_id, question_id),
  KEY idx_user_mastered (user_id, mastered, last_wrong_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本';

-- 8. 会话
CREATE TABLE IF NOT EXISTS chat_session (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  question_id BIGINT UNSIGNED NOT NULL,
  title       VARCHAR(128) NOT NULL DEFAULT '',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_q (user_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 会话';

-- 9. 对话消息（含工具调用，可完整回放时间线）
CREATE TABLE IF NOT EXISTS chat_message (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  session_id  BIGINT UNSIGNED NOT NULL,
  user_id     BIGINT UNSIGNED NOT NULL,
  question_id BIGINT UNSIGNED NOT NULL,
  seq         INT NOT NULL DEFAULT 0,
  role        VARCHAR(16) NOT NULL COMMENT 'user/assistant/tool',
  content     MEDIUMTEXT,
  tool_name   VARCHAR(64) NULL,
  tool_input  JSON NULL,
  tool_output JSON NULL,
  hint_level  TINYINT NOT NULL DEFAULT 0 COMMENT '0无关 1思路 2答案',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_session_seq (session_id, seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息';

-- 10. RAG 文档登记
CREATE TABLE IF NOT EXISTS rag_document (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_type         VARCHAR(32) NOT NULL COMMENT 'SOLUTION/KNOWLEDGE/ERROR_MANUAL/STATEMENT',
  source_id           BIGINT UNSIGNED NULL,
  title               VARCHAR(128) NOT NULL,
  knowledge_point_id  BIGINT UNSIGNED NULL,
  chunk_count         INT NOT NULL DEFAULT 0,
  vector_ids          JSON,
  content_hash        VARCHAR(64) COMMENT '源文变更检测',
  status              VARCHAR(16) NOT NULL DEFAULT 'READY',
  create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG 文档';

-- 11. 文件（P1，可后置）
CREATE TABLE IF NOT EXISTS file_object (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  url         VARCHAR(255) NOT NULL,
  path        VARCHAR(255) NOT NULL,
  mime        VARCHAR(64)  NOT NULL,
  size_bytes  BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上传文件';
