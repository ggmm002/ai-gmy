package com.example.aigmy.controller;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.chat.MessageFormat;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import jakarta.annotation.PostConstruct;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@RestController
public class FirstController {

    private  ChatClient dashScopeChatClient;

    private static final String DEFAULT_PROMPT = "这些是什么？";

    private static final String DEFAULT_VIDEO_PROMPT = "这是一组从视频中提取的图片帧，请描述此视频中的内容。";

    private static final String DEFAULT_AUDIO_PROMPT = "这是一个音频文件，请描述此音频中的内容。";

    private static final String DEFAULT_MODEL = "qwen-vl-max-latest";

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(apiKey)
                .build();

        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen3-vl-plus")
                        .withMultiModel(true)
                        .build())
                .build();
        this.dashScopeChatClient =  ChatClient.create(chatModel);
    }

    @Autowired
    private ReactAgent firstAgent;

    @Autowired
    private ReactAgent vlAgent;

    @GetMapping("/chat")
    public String getChatResponse(@RequestParam("question") String question) {
        Optional<OverAllState> invoke;
        try {
            invoke = firstAgent.invoke(question);
        } catch (GraphRunnerException e) {
            return "Error: " + e.getMessage();
        }
        if (invoke.isPresent()) {
            return invoke.get().toString();
        } else {
            return "No response";
        }
    }

    @GetMapping("/chat2")
    public String getChatResponse2(@RequestParam("question") String question,@RequestParam("userId") Long userId ) {
        Optional<OverAllState> invoke;
        RunnableConfig runnableConfig = RunnableConfig.builder()
        .threadId(userId.toString()) // 暂时先用userId
        .addMetadata("user_id", userId)
                .build();
        try {
            invoke = firstAgent.invoke(question,runnableConfig);
        } catch (GraphRunnerException e) {
            return "Error: " + e.getMessage();
        }
        if (invoke.isPresent()) {
            return invoke.get().toString();
        } else {
            return "No response";
        }
    }

    /**
     * 上传图片并让 AI 解释图片内容
     * 
     * @param image 上传的图片文件
     * @return AI 对图片的解释
     */
    @PostMapping("/analyzeImage")
    public String analyzeImage(@RequestParam("imageUrl") String imageUrl,
                                @RequestParam(value = "message", required = false) String message) {
        try {

            String mineType = determineMimeTypeFromUrl(imageUrl);
            Media media = Media.builder()
                    .data(new URI(imageUrl))
                    .mimeType(MimeTypeUtils.parseMimeType(mineType))
                    .build();
            UserMessage userMessage = UserMessage.builder()
                    .text("描述这张图片的内容。")
                    .media(media)
                    .build();
            
            
            Optional<OverAllState> invoke;
            invoke = vlAgent.invoke(userMessage);
            
            if (invoke.isPresent()) {
                return invoke.get().toString();
            } else {
                return "No response from AI";
            }
            
        } catch (GraphRunnerException e) {
            return "Error: AI处理异常 - " + e.getMessage();
        } catch (Exception e) {
            return "Error: 图片处理失败 - " + e.getMessage();
        }
    }

    @GetMapping("/test")
    public String test() throws MalformedURLException, GraphRunnerException {
        // 从 URL
        UserMessage message = UserMessage.builder()
                .text("描述这张图片的内容。")
                .media(Media.builder()
                        .mimeType(MimeTypeUtils.IMAGE_PNG)
                        .data(new ClassPathResource("img/123.png"))
                        .build())
                .build();

        Optional<OverAllState> invoke = vlAgent.invoke(message);
        if (invoke.isPresent()) {
            return invoke.get().toString();
        } else {
            return "No response from AI";
        }
    }

    @GetMapping("/image")
    public String image(@RequestParam(value = "prompt", required = false, defaultValue = DEFAULT_PROMPT) String prompt)
            throws Exception {

        List<Media> mediaList = List.of(new Media(MimeTypeUtils.IMAGE_PNG,
                new ClassPathResource("img/123.png")));

        UserMessage message =
                UserMessage.builder().text(prompt).media(mediaList).metadata(new HashMap<>()).build();
        message.getMetadata().put(DashScopeApiConstants.MESSAGE_FORMAT, MessageFormat.IMAGE);

        ChatResponse response = dashScopeChatClient
                .prompt(new Prompt(message,
                        DashScopeChatOptions.builder().withModel(DEFAULT_MODEL).withMultiModel(true).build()))
                .call()
                .chatResponse();

        return response.getResult().getOutput().getText();
    }

    private String determineMimeTypeFromUrl(String imageUrl) {
        String lowerUrl = imageUrl.toLowerCase();
        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerUrl.endsWith(".png")) {
            return "image/png";
        } else if (lowerUrl.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerUrl.endsWith(".webp")) {
            return "image/webp";
        } else {
            // 默认使用JPEG
            return "image/jpeg";
        }
    }
    
}
