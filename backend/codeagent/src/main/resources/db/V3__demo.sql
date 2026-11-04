-- CodeAgent 演示数据：演示账号 + 3 道错题 + 12 条提交记录
-- 先执行 V1__init.sql 和 V2__seed.sql，再手工执行本脚本。
-- 账号：demo / Demo@123456
USE codeagent;
SET NAMES utf8mb4;

-- 演示账号（BCrypt 密文对应 Demo@123456）
INSERT INTO sys_user (username, password, nickname, role, status)
VALUES ('demo', '$2a$10$0qvEzIdU730U7xRvqX9Tl.1F3LLDYY4UOSsYctZUoMJSMZ.mRWrky', '演示账号', 'USER', 1)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), status = 1;

SET @demo_id = (SELECT id FROM sys_user WHERE username = 'demo' LIMIT 1);
SET @q_array = (SELECT id FROM question WHERE title = '数组元素求和' AND type = 'PUBLIC' AND deleted = 0 LIMIT 1);
SET @q_palindrome = (SELECT id FROM question WHERE title = '回文串判断' AND type = 'PUBLIC' AND deleted = 0 LIMIT 1);
SET @q_sort = (SELECT id FROM question WHERE title = '数组升序排序' AND type = 'PUBLIC' AND deleted = 0 LIMIT 1);
SET @q_majority = (SELECT id FROM question WHERE title = '多数元素' AND type = 'PUBLIC' AND deleted = 0 LIMIT 1);
SET @q_max = (SELECT id FROM question WHERE title = '递归求数组最大值' AND type = 'PUBLIC' AND deleted = 0 LIMIT 1);

-- 幂等：只清理演示账号自己的历史，避免重复执行后统计翻倍
DELETE FROM user_wrong_question WHERE user_id = @demo_id;
DELETE FROM submit_record WHERE user_id = @demo_id;

-- q1 数组元素求和：2 次 AC + 2 次 WA
INSERT INTO submit_record (user_id, question_id, language, code, status, passed_count, total_count, run_time_ms, create_time)
VALUES (@demo_id, @q_array, 'JAVA', 'public class Main { /* 正确求和 */ }', 'AC', 4, 4, 120, DATE_SUB(NOW(), INTERVAL 11 DAY));
INSERT INTO submit_record (user_id, question_id, language, code, status, passed_count, total_count, run_time_ms, create_time)
VALUES (@demo_id, @q_array, 'JAVA', 'public class Main { /* 正确求和 */ }', 'AC', 4, 4, 98, DATE_SUB(NOW(), INTERVAL 10 DAY));
INSERT INTO submit_record (user_id, question_id, language, code, status, passed_count, total_count, run_time_ms, error_message, create_time)
VALUES (@demo_id, @q_array, 'JAVA', 'public class Main { int sum = 0; /* 少读一个元素 */ }', 'WA', 2, 4, 135, '答案错误', DATE_SUB(NOW(), INTERVAL 9 DAY));
INSERT INTO submit_record (user_id, question_id, language, code, status, passed_count, total_count, run_time_ms, error_message, create_time)
VALUES (@demo_id, @q_array, 'CPP', '#include <bits/stdc++.h>\nint main(){ /* 未初始化 sum */ }', 'WA', 1, 4, 110, '答案错误', DATE_SUB(NOW(), INTERVAL 8 DAY));
SET @wrong_array_last = LAST_INSERT_ID();

-- q4 回文串判断：2 次 AC + 1 次 WA
INSERT INTO submit_record (user_id, question_id, language, code, status, passed_count, total_count, run_time_ms, create_time)
VALUES (@demo_id, @q_palindrome, 'JAVA', 'public class Main { /* 双指针判断回文 */ }', 'AC', 4, 4, 90, DATE_SUB(NOW(), INTERVAL 7 DAY));
INSERT INTO submit_record (user_id, question_id, language, code, status, passed_count, total_count, run_time_ms, error_message, create_time)
VALUES (@demo_id, @q_palindrome, 'JAVA', 'public class Main { /* 只比较前半段 */ }', 'WA', 0, 4, 105, '答案错误', DATE_SUB(NOW(), INTERVAL 6 DAY));
SET @wrong_palindrome_last = LAST_INSERT_ID();
INSERT INTO submit_record (user_id, question_id, language, code, status, passed_count, total_count, run_time_ms, create_time)
VALUES (@demo_id, @q_palindrome, 'JAVA', 'public class Main { /* 双指针判断回文 */ }', 'AC', 4, 4, 88, DATE_SUB(NOW(), INTERVAL 5 DAY));

-- q13 数组升序排序：2 次 AC + 1 次 WA
INSERT INTO submit_record (user_id, question_id, language, code, status, passed_count, total_count, run_time_ms, error_message, create_time)
VALUES (@demo_id, @q_sort, 'CPP', '#include <bits/stdc++.h>\nint main(){ /* 冒泡排序边界错误 */ }', 'WA', 1, 4, 140, '答案错误', DATE_SUB(NOW(), INTERVAL 4 DAY));
SET @wrong_sort_last = LAST_INSERT_ID();
INSERT INTO submit_record (user_id, question_id, language, code, status, passed_count, total_count, run_time_ms, create_time)
VALUES (@demo_id, @q_sort, 'CPP', '#include <bits/stdc++.h>\nint main(){ /* sort */ }', 'AC', 4, 4, 75, DATE_SUB(NOW(), INTERVAL 3 DAY));
INSERT INTO submit_record (user_id, question_id, language, code, status, passed_count, total_count, run_time_ms, create_time)
VALUES (@demo_id, @q_sort, 'JAVA', 'public class Main { /* Arrays.sort */ }', 'AC', 4, 4, 130, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- q2 多数元素：1 次 AC
INSERT INTO submit_record (user_id, question_id, language, code, status, passed_count, total_count, run_time_ms, create_time)
VALUES (@demo_id, @q_majority, 'JAVA', 'public class Main { /* 摩尔投票 */ }', 'AC', 4, 4, 95, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- q10 递归求数组最大值：1 次 AC
INSERT INTO submit_record (user_id, question_id, language, code, status, passed_count, total_count, run_time_ms, create_time)
VALUES (@demo_id, @q_max, 'CPP', '#include <bits/stdc++.h>\nint main(){ /* 递归最大值 */ }', 'AC', 4, 4, 80, NOW());

-- 3 道错题：数组求和、回文判断、排序
INSERT INTO user_wrong_question
  (user_id, question_id, wrong_count, last_submit_id, mastered, first_wrong_time, last_wrong_time)
VALUES
  (@demo_id, @q_array, 2, @wrong_array_last, 0, DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
  (@demo_id, @q_palindrome, 1, @wrong_palindrome_last, 0, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
  (@demo_id, @q_sort, 1, @wrong_sort_last, 0, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY));

-- 校验：应为 12 条提交、3 条错题
SELECT COUNT(*) AS submit_count FROM submit_record WHERE user_id = @demo_id;
SELECT COUNT(*) AS wrong_count FROM user_wrong_question WHERE user_id = @demo_id AND mastered = 0;
