package com.example.aigmy.dify;

import com.alibaba.fastjson.JSON;
import com.example.aigmy.dify.dto.UserQueryRequest;
import com.example.aigmy.dify.service.DifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Dify 工作流控制器
 * 提供对接 Dify API 的接口
 *
 * @author guomaoyang 2025/12/4
 */
@RestController
@RequestMapping("/dify")
@Slf4j
public class DifyController {

    private final DifyService difyService;

    public DifyController(DifyService difyService) {
        this.difyService = difyService;
    }

    /**
     * 流式对话接口（SSE）
     * 支持打字机效果，实时返回AI生成的内容
     *
     * @param request 用户查询请求
     * @return SSE流式响应
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody UserQueryRequest request) {
        log.info("收到流式对话请求: {}", JSON.toJSONString(request));

        // 获取用户ID，如果前端未传递则使用默认值
        // 实际业务中应该从 SecurityContext 或 Token 中获取当前登录用户
        String userId = request.getUserId();
        if (userId == null || userId.isEmpty()) {
            userId = "default_user";
        }

        return difyService.chatStream(
                request.getQuery(),
                request.getConversationId(),
                userId,
                request.getInputs()
        );
    }

    /**
     * 阻塞式对话接口（非流式）
     * 一次性返回完整的AI响应
     *
     * @param request 用户查询请求
     * @return 完整的JSON响应
     */
    @PostMapping("/chat/blocking")
    public String chatBlocking(@RequestBody UserQueryRequest request) {
        log.info("收到阻塞式对话请求: {}", JSON.toJSONString(request));

        // 获取用户ID
        String userId = request.getUserId();
        if (userId == null || userId.isEmpty()) {
            userId = "default_user";
        }

        return difyService.chatBlocking(
                request.getQuery(),
                request.getConversationId(),
                userId,
                request.getInputs()
        );
    }
}
