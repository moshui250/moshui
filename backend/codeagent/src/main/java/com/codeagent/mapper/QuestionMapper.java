package com.codeagent.mapper;

import com.codeagent.dto.QuestionDetailRow;
import com.codeagent.entity.Question;
import com.codeagent.vo.QuestionListItemVO;
import com.codeagent.vo.QuestionRecommendVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface QuestionMapper {

    /**
     * 分页查询题目列表，同时计算当前用户是否已 AC。
     * 对应 SQL 位于 resources/mapper/QuestionMapper.xml。
     *
     * @param personal         是否查询个人题；true 时只查 userId 的 PERSONAL 题
     * @param userId           当前登录用户 ID，用于个人题过滤和 solved 计算
     * @param knowledgePointId 知识点 ID，null 表示不限
     * @param difficulty       难度 1/2/3，null 表示不限
     * @param keyword          标题关键字，null 或空串表示不限
     * @param offset           分页偏移量
     * @param size             每页条数
     * @return 题目列表
     */
    List<QuestionListItemVO> selectList(@Param("personal") boolean personal,
                                        @Param("userId") Long userId,
                                        @Param("knowledgePointId") Long knowledgePointId,
                                        @Param("difficulty") Integer difficulty,
                                        @Param("keyword") String keyword,
                                        @Param("offset") long offset,
                                        @Param("size") long size);

    /**
     * 统计题目列表总数，筛选条件与 selectList 保持一致。
     *
     * @param personal         是否统计个人题
     * @param userId           当前登录用户 ID
     * @param knowledgePointId 知识点 ID，null 表示不限
     * @param difficulty       难度 1/2/3，null 表示不限
     * @param keyword          标题关键字，null 或空串表示不限
     * @return 符合条件的题目总数
     */
    long countList(@Param("personal") boolean personal,
                   @Param("userId") Long userId,
                   @Param("knowledgePointId") Long knowledgePointId,
                   @Param("difficulty") Integer difficulty,
                   @Param("keyword") String keyword);

    /**
     * 查询题目详情行，关联知识点名称和编码。
     *
     * @param id 题目 ID
     * @return 题目详情行；不存在时返回 null
     */
    QuestionDetailRow selectDetailRow(@Param("id") Long id);

    /**
     * 按标题模糊匹配公开题，用于向量库不可用时的关键词降级检索。
     *
     * @param keyword 已清洗的查询词
     * @return 最多 4 道匹配题目
     */
    @Select("SELECT id, title, description, difficulty, knowledge_point_id, type, user_id " +
            "FROM question WHERE deleted = 0 AND type = 'PUBLIC' AND title LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY id ASC LIMIT 4")
    List<Question> selectByTitleLike(@Param("keyword") String keyword);

    /**
     * 新增个人题，并回填自增主键。
     *
     * @param question 题目实体，type 固定写入 PERSONAL
     * @return 受影响行数
     */
    int insert(Question question);

    /**
     * 更新当前用户自己的个人题，SQL 再次校验 type 和 user_id。
     *
     * @param question 待更新题目，必须包含 id；userId 用于权限条件
     * @return 受影响行数，0 表示题目不存在或无权限
     */
    int updateOwned(Question question);

    /**
     * 逻辑删除当前用户自己的个人题。
     *
     * @param id     题目 ID
     * @param userId 当前登录用户 ID
     * @return 受影响行数，0 表示题目不存在或无权限
     */
    int logicalDeleteOwned(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 推荐公开题：同知识点优先，再按未做过、难度距离排序。
     * 每个知识点只有 3 题，排除本题后同知识点可能不足 limit 条，
     * 此时用其他知识点的相近难度题补齐，保证推荐数量。
     *
     * @param userId           当前登录用户 ID，用于计算 solved
     * @param knowledgePointId 当前题的知识点 ID，同知识点排在最前
     * @param difficulty       当前题难度，差值越小越优先
     * @param excludeId        要排除的当前题 ID
     * @param limit            最多返回条数
     * @return 推荐题目列表
     */
    List<QuestionRecommendVO> selectRecommend(@Param("userId") Long userId,
                                              @Param("knowledgePointId") Long knowledgePointId,
                                              @Param("difficulty") Integer difficulty,
                                              @Param("excludeId") Long excludeId,
                                              @Param("limit") int limit);
}
