package com.codeagent.mapper;

import com.codeagent.entity.ChatMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 对话消息 Mapper。
 */
public interface ChatMessageMapper {

    /**
     * 查询会话内当前最大序号。
     *
     * @param sessionId 会话 ID
     * @return 最大 seq；没有消息时返回 0
     */
    @Select("SELECT COALESCE(MAX(seq), 0) FROM chat_message WHERE session_id = #{sessionId}")
    int selectMaxSeq(@Param("sessionId") Long sessionId);

    /**
     * 写入一条消息并回填自增主键。
     *
     * @param message 消息实体
     * @return 受影响行数
     */
    @Insert("INSERT INTO chat_message(session_id, user_id, question_id, seq, role, content, " +
            "tool_name, tool_input, tool_output, hint_level, create_time) " +
            "VALUES(#{sessionId}, #{userId}, #{questionId}, #{seq}, #{role}, #{content}, " +
            "#{toolName}, #{toolInput}, #{toolOutput}, #{hintLevel}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessage message);

    /**
     * 更新助手占位消息的最终正文。
     *
     * @param id      消息 ID
     * @param content 最终回答
     * @return 受影响行数
     */
    @Update("UPDATE chat_message SET content = #{content} WHERE id = #{id}")
    int updateContent(@Param("id") Long id, @Param("content") String content);

    /**
     * 查询用户在某道题下的全部消息，按会话和序号排序。
     *
     * @param userId     用户 ID
     * @param questionId 题目 ID
     * @return 消息列表
     */
    @Select("SELECT id, session_id, user_id, question_id, seq, role, content, tool_name, " +
            "tool_input, tool_output, hint_level, create_time " +
            "FROM chat_message WHERE user_id = #{userId} AND question_id = #{questionId} " +
            "ORDER BY session_id ASC, seq ASC, id ASC")
    List<ChatMessage> selectByUserAndQuestion(@Param("userId") Long userId,
                                              @Param("questionId") Long questionId);
}
