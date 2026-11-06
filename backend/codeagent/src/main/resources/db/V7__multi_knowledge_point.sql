-- V7: 题目多知识点关联，并补足常用算法知识点
CREATE TABLE IF NOT EXISTS question_knowledge_point (
    question_id        BIGINT UNSIGNED NOT NULL,
    knowledge_point_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (question_id, knowledge_point_id),
    KEY idx_qkp_point (knowledge_point_id),
    KEY idx_qkp_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='题目与知识点多对多关联';

INSERT IGNORE INTO question_knowledge_point (question_id, knowledge_point_id)
SELECT id, knowledge_point_id FROM question WHERE knowledge_point_id IS NOT NULL AND deleted = 0;

INSERT INTO knowledge_point (code, name, description, sort, create_time)
VALUES
    ('algorithm', '算法', '复杂度、贪心、二分、双指针与常见算法思想。', 6, NOW()),
    ('tree', '树', '二叉树、遍历、二叉搜索树与树的递归结构。', 7, NOW()),
    ('graph', '图', '图的表示、遍历、最短路与连通性。', 8, NOW()),
    ('dp', '动态规划', '状态定义、转移、背包与序列型动态规划。', 9, NOW()),
    ('math', '数学', '数论、组合、进制与常见数学推导。', 10, NOW()),
    ('hash', '哈希表', '哈希映射、计数、去重与频次统计。', 11, NOW())
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    sort = VALUES(sort);
