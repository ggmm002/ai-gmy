package com.example.aigmy.controller;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.chat.MessageFormat;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/hitl")
@Slf4j
public class HitlController {


    @Autowired
    private ReactAgent hitlAgent;


    @GetMapping("/chat")
    public String chat(@RequestParam("question") String question,@RequestParam("userId") Long userId ) throws GraphRunnerException {
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId(userId.toString()) // 暂时先用userId
                .addMetadata("user_id", userId)
                .build();
        Optional<NodeOutput> result = hitlAgent.invokeAndGetOutput(question, runnableConfig);
        // 5. 检查中断并处理
        if (result.isPresent() && result.get() instanceof InterruptionMetadata) {
            InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();

            log.info("检测到中断，需要人工审批");
            List<InterruptionMetadata.ToolFeedback> toolFeedbacks =
                    interruptionMetadata.toolFeedbacks();

            for (InterruptionMetadata.ToolFeedback feedback : toolFeedbacks) {
                log.info("工具:({}) " , feedback.getName());
                log.info("参数: ({}) " , feedback.getArguments());
                log.info("描述: ({}) " , feedback.getDescription());
            }

            // 6. 模拟人工决策（这里选择批准）
            InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
                    .nodeId(interruptionMetadata.node())
                    .state(interruptionMetadata.state());

            toolFeedbacks.forEach(toolFeedback -> {
                InterruptionMetadata.ToolFeedback approvedFeedback =
                        InterruptionMetadata.ToolFeedback.builder(toolFeedback)
                                .result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED)
                                .build();
                feedbackBuilder.addToolFeedback(approvedFeedback);
            });

            InterruptionMetadata approvalMetadata = feedbackBuilder.build();

            // 7. 第二次调用 - 使用人工反馈恢复执行
            log.info("第二次调用：使用批准决策恢复 ===");
                    RunnableConfig resumeConfig = RunnableConfig.builder()
                            .threadId(userId.toString())
                            .addMetadata("user_id", userId)
                            .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, approvalMetadata)
                            .build();

            result = hitlAgent.invokeAndGetOutput("", resumeConfig);

//            if (finalResult.isPresent()) {
//                System.out.println("执行完成");
//                System.out.println("最终结果: " + finalResult.get());
//            }
        }
        return result.toString();
    }
}
