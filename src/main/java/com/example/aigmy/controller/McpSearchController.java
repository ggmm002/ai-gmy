package com.example.aigmy.controller;

import com.alibaba.cloud.ai.agent.studio.dto.messages.AgentRunResponse;
import com.alibaba.cloud.ai.agent.studio.dto.messages.MessageDTO;
import com.alibaba.cloud.ai.agent.studio.dto.messages.ToolRequestConfirmMessageDTO;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Optional;

/**
 * MCP 互联网搜索控制器
 * 提供基于 MCP (Model Context Protocol) 的互联网搜索功能
 * 集成 bing-cn-mcp 服务，实现智能搜索问答
 *
 * @author guomaoyang 2025/12/1
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
public class McpSearchController {

    @Autowired
    @Qualifier("mcpSearchAgent")
    private ReactAgent mcpSearchAgent;

    final ObjectMapper mapper = new ObjectMapper();

    /**
     * MCP 搜索接口
     * 使用互联网搜索获取最新信息并回答问题
     *
     * @param question 用户问题
     * @return 搜索结果和 AI 回答
     */
    @GetMapping("/search")
    public Flux<ServerSentEvent<String>> search(@RequestParam String question) {
        log.info("收到 MCP 搜索请求，问题: {}", question);

        try {
            RunnableConfig runnableConfig = RunnableConfig.builder()
                    .threadId("1")
                    .addMetadata("user_id", "1")
                    .build();

            return executeAgent(UserMessage.builder().text(question).build(), mcpSearchAgent, runnableConfig);
        }
        catch (Exception e) {
            return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Agent run failed", e));
        }
    }

    /**
     * MCP 搜索接口（带用户会话）
     * 支持多轮对话的互联网搜索
     *
     * @param question 用户问题
     * @param userId   用户ID（用于会话隔离）
     * @return 搜索结果和 AI 回答
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String question, @RequestParam Long userId) {
        log.info("收到 MCP 对话搜索请求，用户: {}, 问题: {}", userId, question);
        try {
            RunnableConfig runnableConfig = RunnableConfig.builder()
                    .threadId("mcp_search_" + userId)
                    .addMetadata("user_id", userId)
                    .build();
            Optional<OverAllState> result = mcpSearchAgent.invoke(question, runnableConfig);
            if (result.isPresent()) {
                log.info("MCP 对话搜索结果: {}", JSON.toJSONString(result.get()));
                return result.get().toString();
            } else {
                return "未获取到搜索结果";
            }
        } catch (GraphRunnerException e) {
            log.error("MCP 对话搜索失败", e);
            return "搜索失败: " + e.getMessage();
        }
    }

    /**
     * 健康检查接口
     * 检查 MCP 服务是否正常
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    public String health() {
        if (mcpSearchAgent != null) {
            return "MCP Search Agent 已就绪";
        } else {
            return "MCP Search Agent 未初始化";
        }
    }

    @NotNull
    private Flux<ServerSentEvent<String>> executeAgent(UserMessage userMessage, BaseAgent agent, RunnableConfig runnableConfig) throws GraphRunnerException {

        log.info("开始执行 Agent stream，threadId: {}", runnableConfig.threadId());
        log.info("RunnableConfig 元数据: {}", JSON.toJSONString(runnableConfig.metadata()));

        Flux<NodeOutput> agentStream;

        if (userMessage != null) {
            log.info("使用 UserMessage 启动 stream，内容: {}", userMessage.getText());
            agentStream = agent.stream(userMessage, runnableConfig);
        }
        else {
            log.info("使用空字符串启动 stream");
            agentStream = agent.stream("", runnableConfig);
        }

        // Convert Flux<NodeOutput> to Flux<ServerSentEvent<String>>
        // 添加超时处理，防止stream无限期卡住（5分钟超时）
        return agentStream
                .timeout(Duration.ofMinutes(5))
                .doOnSubscribe(subscription -> {
                    log.info("Agent stream 已订阅");
                })
                .doOnNext(nodeOutput -> {
                    log.info("收到 NodeOutput，类型: {}, node: {}, agent: {}", 
                            nodeOutput.getClass().getSimpleName(), 
                            nodeOutput.node(), 
                            nodeOutput.agent());
                })
                .doOnCancel(() -> {
                    log.warn("Agent stream 被取消");
                })
                .map(nodeOutput -> {
                    String node = nodeOutput.node();
                    String agentName = nodeOutput.agent();
                    Usage tokenUsage = nodeOutput.tokenUsage();

                    log.debug("处理 NodeOutput - node: {}, agent: {}, tokenUsage: {}", 
                            node, agentName, JSON.toJSONString(tokenUsage));

                    // For streaming, we can use the message content as chunk
                    StringBuilder chunkBuilder = new StringBuilder();
                    AgentRunResponse agentResponse = null;
                    if (nodeOutput instanceof StreamingOutput<?> streamingOutput) {
                        Message message = streamingOutput.message();
                        if (message == null) { // no update, typically output responses from nodes that does not produce messages
                            log.debug("StreamingOutput 消息为空，返回空响应");
                            return ServerSentEvent.<String>builder()
                                    .data("{}")
                                    .build();
                        }
                        if (message instanceof AssistantMessage assistantMessage) {
                            if (assistantMessage.hasToolCalls()) {
                                log.info("检测到工具调用，工具数量: {}", assistantMessage.getToolCalls().size());
                                assistantMessage.getToolCalls().forEach(toolCall -> {
                                    log.info("工具调用详情: {}", toolCall);
                                });
                                agentResponse = new AgentRunResponse(node, agentName, assistantMessage, tokenUsage, "");
                            }
                            else {
                                log.debug("AssistantMessage 无工具调用，文本内容: {}", assistantMessage.getText());
//						chunkBuilder.append(assistantMessage.getText());
                                agentResponse = new AgentRunResponse(node, agentName, assistantMessage, tokenUsage, assistantMessage.getText());
                            }
                        }
                        else {
                            log.debug("StreamingOutput 消息类型: {}", message.getClass().getSimpleName());
                            agentResponse = new AgentRunResponse(node, agentName, message, tokenUsage, "");
                        }
                    }
                    else if (nodeOutput instanceof InterruptionMetadata interruptionMetadata) {
                        log.info("检测到 InterruptionMetadata，需要人工反馈");
                        log.info("InterruptionMetadata 详情: {}", JSON.toJSONString(interruptionMetadata));
                        // Use the specialized method to convert InterruptionMetadata to ToolRequestMessageDTO
                        ToolRequestConfirmMessageDTO toolRequestMessage = MessageDTO.MessageDTOFactory.fromInterruptionMetadata(interruptionMetadata);
                        agentResponse = new AgentRunResponse(node, agentName, toolRequestMessage, tokenUsage, "");
                    }
                    else {
                        log.debug("其他类型的 NodeOutput: {}", nodeOutput.getClass().getSimpleName());
                        // Handle other NodeOutput types if necessary
//					agentResponse = new AgentRunResponse(node, agentName, null, tokenUsage, "");
                    }


                    // Serialize to JSON string
                    try {
                        if (agentResponse != null) {
                            String jsonData = mapper.writeValueAsString(agentResponse);
                            log.debug("序列化 AgentRunResponse 成功，数据长度: {}", jsonData.length());
                            return ServerSentEvent.<String>builder()
                                    .data(jsonData)
                                    .build();
                        }
                        else {
                            log.warn("AgentResponse 为 null，返回空响应");
                        }
                    }
                    catch (Exception e) {
                        log.error("Failed to serialize AgentRunResponse to JSON", e);
                        return ServerSentEvent.<String>builder()
                                .data("{\"error\":\"Failed to serialize response\"}")
                                .build();
                    }
                    return ServerSentEvent.<String>builder()
                            .data("{}")
                            .build();
                })
                .doOnComplete(() -> {
                    log.info("Agent stream 执行完成");
                })
                .doOnError(error -> {
                    log.error("Agent stream 执行出错，错误类型: {}, 错误信息: {}", 
                            error.getClass().getSimpleName(), 
                            error.getMessage(), 
                            error);
                })
                .onErrorResume(error -> {
                    // Handle errors from the agent stream and convert to SSE error event
                    log.error("Error occurred during agent stream execution", error);

                    // Create error response
                    String errorMessage = error.getMessage() != null ? error.getMessage() : "Unknown error occurred";
                    String errorType = error.getClass().getSimpleName();

                    try {
                        // Create a structured error response
                        String errorJson = String.format(
                                "{\"error\":true,\"errorType\":\"%s\",\"errorMessage\":\"%s\"}",
                                errorType.replace("\"", "\\\""),
                                errorMessage.replace("\"", "\\\"").replace("\n", "\\n")
                        );

                        // Return the error as an SSE event and complete the stream
                        return Flux.just(
                                ServerSentEvent.<String>builder()
                                        .event("error")
                                        .data(errorJson)
                                        .build()
                        );
                    }
                    catch (Exception e) {
                        log.error("Failed to create error SSE event", e);
                        return Flux.just(
                                ServerSentEvent.<String>builder()
                                        .event("error")
                                        .data("{\"error\":true,\"errorMessage\":\"Internal error occurred\"}")
                                        .build()
                        );
                    }
                });
    }
}
