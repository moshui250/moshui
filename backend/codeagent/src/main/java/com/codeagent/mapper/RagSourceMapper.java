package com.codeagent.mapper;

import com.codeagent.ai.rag.RagSourceDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 从业务表读取 RAG 语料，只做只读查询。
 */
@Mapper
public interface RagSourceMapper {

    /**
     * 读取全部题解（C++/Java 各一条），拼成带思路和代码的 Markdown。
     *
     * @return 题解来源文档，按 solution id 升序
     */
    @Select("""
            SELECT 'SOLUTION' AS source_type,
                   s.id AS source_id,
                   CONCAT(q.title, ' · ', s.language, ' 题解') AS title,
                   q.knowledge_point_id AS knowledge_point_id,
                   kp.code AS knowledge_point_code,
                   CONCAT('# ', q.title, ' · ', s.language, ' 题解', CHAR(10), CHAR(10),
                          '## 思路', CHAR(10), COALESCE(s.explanation, ''), CHAR(10), CHAR(10),
                          '## 参考代码', CHAR(10), '```', LOWER(s.language), CHAR(10),
                          COALESCE(s.content, ''), CHAR(10), '```') AS content
            FROM question_solution s
            JOIN question q ON q.id = s.question_id AND q.deleted = 0
            JOIN knowledge_point kp ON kp.id = q.knowledge_point_id
            ORDER BY s.id
            """)
    List<RagSourceDocument> selectSolutions();

    /**
     * 读取全部知识点，拼成 Markdown 文档。
     *
     * @return 知识点来源文档，按 sort、id 升序
     */
    @Select("""
            SELECT 'KNOWLEDGE' AS source_type,
                   kp.id AS source_id,
                   CONCAT(kp.name, ' 知识点') AS title,
                   kp.id AS knowledge_point_id,
                   kp.code AS knowledge_point_code,
                   CONCAT('# ', kp.name, ' 知识点', CHAR(10), CHAR(10),
                          COALESCE(kp.description, '')) AS content
            FROM knowledge_point kp
            ORDER BY kp.sort, kp.id
            """)
    List<RagSourceDocument> selectKnowledgePoints();

    /**
     * 读取全部未删除题目的题面，拼成 Markdown 文档。
     *
     * @return 题面来源文档，按 question id 升序
     */
    @Select("""
            SELECT 'STATEMENT' AS source_type,
                   q.id AS source_id,
                   CONCAT(q.title, ' 题目') AS title,
                   q.knowledge_point_id AS knowledge_point_id,
                   kp.code AS knowledge_point_code,
                   CONCAT('# ', q.title, CHAR(10), CHAR(10),
                          COALESCE(q.description, ''), CHAR(10), CHAR(10),
                          '## 输入格式', CHAR(10), COALESCE(q.input_desc, ''), CHAR(10), CHAR(10),
                          '## 输出格式', CHAR(10), COALESCE(q.output_desc, '')) AS content
            FROM question q
            JOIN knowledge_point kp ON kp.id = q.knowledge_point_id
            WHERE q.deleted = 0
            ORDER BY q.id
            """)
    List<RagSourceDocument> selectStatements();
}
