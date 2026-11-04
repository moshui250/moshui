package com.codeagent.mapper;

import com.codeagent.entity.RagDocument;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * rag_document 登记表 Mapper，负责内容哈希与向量 ID 的幂等登记。
 */
public interface RagDocumentMapper {

    /**
     * 按来源类型与来源 ID 查询登记记录。
     *
     * @param sourceType 来源类型，如 SOLUTION/KNOWLEDGE/ERROR_MANUAL/STATEMENT
     * @param sourceId   来源主键；ERROR_MANUAL 固定为 0
     * @return 已登记记录，不存在时返回 null
     */
    @Select("""
            SELECT id, source_type, source_id, title, knowledge_point_id,
                   chunk_count, vector_ids, content_hash, status, create_time, update_time
            FROM rag_document
            WHERE source_type = #{sourceType} AND source_id = #{sourceId}
            LIMIT 1
            """)
    RagDocument selectBySource(@Param("sourceType") String sourceType,
                               @Param("sourceId") Long sourceId);

    /**
     * 插入新的 RAG 文档登记记录。
     *
     * @param document 文档登记信息；id 由数据库自增回填
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO rag_document
                (source_type, source_id, title, knowledge_point_id, chunk_count,
                 vector_ids, content_hash, status, create_time, update_time)
            VALUES
                (#{sourceType}, #{sourceId}, #{title}, #{knowledgePointId}, #{chunkCount},
                 #{vectorIds}, #{contentHash}, #{status}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RagDocument document);

    /**
     * 更新已有登记记录的内容哈希、块数与向量 ID。
     *
     * @param document 含 id 的登记信息
     * @return 影响行数
     */
    @Update("""
            UPDATE rag_document
            SET title = #{title},
                knowledge_point_id = #{knowledgePointId},
                chunk_count = #{chunkCount},
                vector_ids = #{vectorIds},
                content_hash = #{contentHash},
                status = #{status},
                update_time = NOW()
            WHERE id = #{id}
            """)
    int update(RagDocument document);
}
