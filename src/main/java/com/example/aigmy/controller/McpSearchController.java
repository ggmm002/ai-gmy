package com.example.aigmy.controller;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * MCP 搜索接口
     * 使用互联网搜索获取最新信息并回答问题
     *
     * @param question 用户问题
     * @return 搜索结果和 AI 回答
     */
    @GetMapping("/search")
    public String search(@RequestParam String question) {
        log.info("收到 MCP 搜索请求，问题: {}", question);
        try {
            Optional<OverAllState> result = mcpSearchAgent.invoke(question);
            if (result.isPresent()) {
                log.info("MCP 搜索结果: {}", JSON.toJSONString(result));
                return result.toString();
            } else {
                return "未获取到搜索结果";
                }
        } catch (GraphRunnerException e) {
            log.error("MCP 搜索失败", e);
            return "搜索失败: " + e.getMessage();
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
}
