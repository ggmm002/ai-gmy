package com.example.aigmy.interceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author guomaoyang 2025/12/1
 */
@Slf4j
@Component
public class MyToolsInceptor extends ToolInterceptor {
    @Override
    public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
        log.info("调用tool，toolName:{}",request.getToolName());

        return handler.call(request);
    }

    @Override
    public String getName() {
        return "MyToolsInceptor";
    }
}
