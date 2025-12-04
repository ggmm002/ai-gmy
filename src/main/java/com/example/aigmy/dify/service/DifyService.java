package com.example.aigmy.dify.service;

import com.alibaba.fastjson.JSON;
import com.example.aigmy.dify.dto.DifyChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

/**
 * Dify API 服务类
 * 负责与 Dify API 进行通信
 *
 * @author guomaoyang 2025/12/4
 */
@Service
@Slf4j
public class DifyService {

    private final WebClient webClient;

    @Value("${dify.timeout:180}")
    private int timeout;

    public DifyService(
            WebClient.Builder webClientBuilder,
            @Value("${dify.api-key}") String difyApiKey,
            @Value("${dify.base-url}") String difyBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(difyBaseUrl)
                .defaultHeader("Authorization", "Bearer " + difyApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        log.info("DifyService 初始化完成, baseUrl: {}", difyBaseUrl);
    }

    /**
     * 发送流式对话请求
     *
     * @param query          用户问题
     * @param conversationId 会话ID (如果是新会话则为 null)
     * @param userId         业务系统当前用户ID
     * @param inputs         提示词变量（可选）
     * @return Flux<String> Dify返回的SSE流字符串
     */
    public Flux<String> chatStream(String query, String conversationId, String userId, Map<String, Object> inputs) {
        DifyChatRequest request = DifyChatRequest.builder()
                .inputs(inputs != null ? inputs : Map.of())
                .query(query)
                .responseMode("streaming")
                .conversationId(conversationId)
                .user(userId)
                .build();

        log.info("发送Dify流式请求, request: {}", JSON.toJSONString(request));

        return webClient.post()
                .uri("/chat-messages")
                .bodyValue(request)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(timeout))
                .doOnError(error -> log.error("Dify请求异常: {}", error.getMessage()))
                .doOnComplete(() -> log.info("Dify流式响应完成"));
    }

    /**
     * 发送阻塞式对话请求（非流式）
     *
     * @param query          用户问题
     * @param conversationId 会话ID (如果是新会话则为 null)
     * @param userId         业务系统当前用户ID
     * @param inputs         提示词变量（可选）
     * @return String Dify返回的完整响应JSON
     */
    public String chatBlocking(String query, String conversationId, String userId, Map<String, Object> inputs) {
        DifyChatRequest request = DifyChatRequest.builder()
                .inputs(inputs != null ? inputs : Map.of())
                .query(query)
                .responseMode("blocking")
                .conversationId(conversationId)
                .user(userId)
                .build();

        log.info("发送Dify阻塞请求, request: {}", JSON.toJSONString(request));

        String response = webClient.post()
                .uri("/chat-messages")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeout))
                .doOnError(error -> log.error("Dify请求异常: {}", error.getMessage()))
                .block();

        log.info("Dify阻塞响应: {}", response);
        return response;
    }
}
