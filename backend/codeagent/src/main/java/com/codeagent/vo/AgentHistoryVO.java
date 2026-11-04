package com.codeagent.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 某道题下的 Agent 历史回放，按会话分组。
 */
@Data
public class AgentHistoryVO {

    /**
     * 会话列表，最近更新的在前。
     */
    private List<Session> sessions = new ArrayList<>();

    /**
     * 一次会话及其消息。
     */
    @Data
    public static class Session {

        /**
         * 会话 ID。
         */
        private Long id;

        /**
         * 会话标题。
         */
        private String title;

        /**
         * 创建时间。
         */
        private LocalDateTime createTime;

        /**
         * 更新时间。
         */
        private LocalDateTime updateTime;

        /**
         * 会话内消息，按 seq 升序。
         */
        private List<Message> messages = new ArrayList<>();
    }

    /**
     * 一条可回放消息。
     */
    @Data
    public static class Message {

        /**
         * 消息 ID。
         */
        private Long id;

        /**
         * 角色：user / assistant / tool。
         */
        private String role;

        /**
         * 消息正文；tool 消息为摘要。
         */
        private String content;

        /**
         * 工具名；非 tool 消息为空。
         */
        private String toolName;

        /**
         * 工具入参 JSON。
         */
        private JsonNode toolInput;

        /**
         * 工具输出 JSON。
         */
        private JsonNode toolOutput;

        /**
         * 提示层级：1 提示，2 参考代码。
         */
        private Integer hintLevel;

        /**
         * 创建时间。
         */
        private LocalDateTime createTime;
    }
}
