package com.example.aigmy.dify.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Dify 聊天请求 DTO
 * 用于映射 Dify API 的请求格式
 *
 * @author guomaoyang 2025/12/4
 */
@Data
@Builder
public class DifyChatRequest {

    /**
     * 提示词变量，如果Prompt没有变量，传空Map
     */
    private Map<String, Object> inputs;

    /**
     * 用户提问内容
     */
    private String query;

    /**
     * 响应模式: "streaming" 流式 或 "blocking" 阻塞
     */
    @JsonProperty("response_mode")
    private String responseMode;

    /**
     * 会话ID，由Dify生成，第一次传空或null
     * 用于保持多轮对话的上下文
     */
    @JsonProperty("conversation_id")
    private String conversationId;

    /**
     * 业务系统的用户ID (必填)
     * 用于Dify区分不同用户的上下文
     */
    private String user;

    /**
     * 文件信息，如果涉及图片上传
     */
    private Map<String, Object> files;
}
