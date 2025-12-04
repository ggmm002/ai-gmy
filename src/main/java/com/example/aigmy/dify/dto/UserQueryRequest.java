package com.example.aigmy.dify.dto;

import lombok.Data;

import java.util.Map;

/**
 * 用户查询请求 DTO
 * 用于前端调用后端的请求格式
 *
 * @author guomaoyang 2025/12/4
 */
@Data
public class UserQueryRequest {

    /**
     * 用户提问内容
     */
    private String query;

    /**
     * 会话ID，首次对话可为空
     * 后续对话需要传递Dify返回的conversation_id以保持上下文
     */
    private String conversationId;

    /**
     * 用户ID，如果前端不传，则后端自动获取
     */
    private String userId;

    /**
     * 提示词变量（可选）
     */
    private Map<String, Object> inputs;
}
