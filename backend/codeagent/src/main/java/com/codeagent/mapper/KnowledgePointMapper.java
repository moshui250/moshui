package com.codeagent.mapper;

import com.codeagent.entity.KnowledgePoint;
import com.codeagent.vo.KnowledgePointVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface KnowledgePointMapper {

    /**
     * 一次 left join + group by 统计题量，避免逐个知识点 count 的 N+1。
     * 联表统计 SQL 位于 resources/mapper/KnowledgePointMapper.xml。
     *
     * @return 知识点列表，每项包含公开题数量
     */
    List<KnowledgePointVO> selectListWithQuestionCount();

    /**
     * 按 ID 查询知识点，用于创建/修改题目时校验。
     *
     * @param id 知识点 ID
     * @return 知识点；不存在时返回 null
     */
    @Select("SELECT id, code, name, description, sort, create_time " +
            "FROM knowledge_point WHERE id = #{id} LIMIT 1")
    KnowledgePoint selectById(@Param("id") Long id);

    /**
     * 按名称模糊匹配知识点，用于向量库不可用时的关键词降级检索。
     *
     * @param keyword 已清洗的查询词
     * @return 最多 4 个匹配知识点
     */
    @Select("SELECT id, code, name, description, sort, create_time " +
            "FROM knowledge_point WHERE name LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY sort ASC, id ASC LIMIT 4")
    List<KnowledgePoint> selectByNameLike(@Param("keyword") String keyword);
}
