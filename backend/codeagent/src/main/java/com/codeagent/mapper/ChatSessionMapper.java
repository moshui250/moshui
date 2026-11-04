package com.codeagent.mapper;

import com.codeagent.entity.ChatSession;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 对话会话 Mapper。
 * 一个用户在同一道题下复用最近一次会话。
 */
public interface ChatSessionMapper {

    /**
     * 查询用户在某道题下最近的会话。
     *
     * @param userId     用户 ID
     * @param questionId 题目 ID
     * @return 最近会话；不存在时返回 null
     */
    @Select("SELECT id, user_id, question_id, title, create_time, update_time " +
            "FROM chat_session WHERE user_id = #{userId} AND question_id = #{questionId} " +
            "ORDER BY update_time DESC, id DESC LIMIT 1")
    ChatSession selectLatest(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /**
     * 新建会话并回填自增主键。
     *
     * @param session 会话实体
     * @return 受影响行数
     */
    @Insert("INSERT INTO chat_session(user_id, question_id, title, create_time, update_time) " +
            "VALUES(#{userId}, #{questionId}, #{title}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatSession session);

    /**
     * 刷新会话的更新时间。
     *
     * @param id 会话 ID
     * @return 受影响行数
     */
    @Update("UPDATE chat_session SET update_time = NOW() WHERE id = #{id}")
    int touch(@Param("id") Long id);
}
