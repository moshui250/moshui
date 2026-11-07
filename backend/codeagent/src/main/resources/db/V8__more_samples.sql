-- V8：为每道题补充更多公开示例。
-- 复用已有的隐藏用例作为新的公开样例，不改变隐藏用例本身；
-- 这样每道题从 2 条示例增加到 4 条，判题侧仍保留原有隐藏覆盖。
-- 可重复执行：已存在相同输入/输出的示例不会重复插入。
INSERT INTO question_test_case (question_id, input_data, expected_output, is_sample, score, sort_no)
SELECT
    c.question_id,
    c.input_data,
    c.expected_output,
    1,
    0,
    100 + ROW_NUMBER() OVER (PARTITION BY c.question_id ORDER BY c.sort_no)
FROM question_test_case c
WHERE c.is_sample = 0
  AND NOT EXISTS (
      SELECT 1
      FROM question_test_case s
      WHERE s.question_id = c.question_id
        AND s.is_sample = 1
        AND s.input_data = c.input_data
        AND s.expected_output = c.expected_output
  );
