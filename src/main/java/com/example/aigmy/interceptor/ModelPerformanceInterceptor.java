package com.example.aigmy.interceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

/**
 * @author guomaoyang 2025/11/23
 */
@Component
@Slf4j
public class ModelPerformanceInterceptor extends ModelInterceptor {
    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        log.info("调用消息条数: {}", request.getMessages().size());

        long startTime = System.currentTimeMillis();
        ModelResponse response = handler.call(request);
        long endTime = System.currentTimeMillis();
        log.info("模型响应耗时: {}ms", endTime - startTime);

        return response;
    }

    @Override
    public String getName() {
        return "ModelPerformanceInterceptor";
    }
}
