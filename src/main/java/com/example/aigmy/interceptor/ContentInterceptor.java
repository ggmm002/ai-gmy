package com.example.aigmy.interceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author guomaoyang 2025/11/23
 */
@Component
public class ContentInterceptor extends ModelInterceptor {
    private static final List<String> BLOCKED_WORDS =
            List.of("草泥马", "敏感词2", "敏感词3");

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        for (Message message : request.getMessages()) {
            String text = message.getText().toLowerCase();
            for (String blockedWord : BLOCKED_WORDS) {
                if (text.contains(blockedWord)) {
                    return ModelResponse.of(new AssistantMessage("检测到不适当的内容，请修改您的输入"));
                }
            }
        }

        ModelResponse response = handler.call(request);

        return response;
    }

    @Override
    public String getName() {
        return "ContentInterceptor";
    }
}
