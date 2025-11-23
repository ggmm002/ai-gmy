package com.example.aigmy.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.contextediting.ContextEditingInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.example.aigmy.interceptor.ContentInterceptor;
import com.example.aigmy.interceptor.ModelPerformanceInterceptor;
import com.example.aigmy.tool.CarBrandTool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author guomaoyang 2025/11/22
 */
@Configuration
public class MyAgentConfiguration {

    private static final String SYSTEM_PROMPT = """
    你是一个汽车门店的销售经理，你的任务是根据用户的问题，给出相应的回答。
    用户的问题可能是：
    1. 你们店有哪些汽车品牌？
    2. 你们店有哪些汽车型号？
    3. 你们店有哪些汽车价格？
    4. 你们店有哪些汽车颜色？
    5. 你们店有哪些汽车配置？
    6. 你们店有哪些汽车优惠政策？
    """;

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Autowired
    private ContentInterceptor contentInterceptor;

    @Autowired
    private ModelPerformanceInterceptor modelPerformanceInterceptor;

    @Bean("firstAgent")
    public ReactAgent firstAgent(){
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(apiKey)
                .build();

        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen-max")
                        .withTemperature(0.5)
                        .withMaxToken(1000)
                        .build())
                .build();

        ToolCallback carBrandTool = FunctionToolCallback
                .builder("carBrandTool", new CarBrandTool())
                .description("查询销售的汽车品牌")
                .build();

        ContextEditingInterceptor.builder();

        return ReactAgent.builder()
                .name("weather_pun_agent")
                .model(chatModel)
                .systemPrompt(SYSTEM_PROMPT)
//                .tools(getUserLocationTool, getWeatherTool)
//                .outputType(ResponseFormat.class)
                .tools(carBrandTool)
                .interceptors(contentInterceptor,modelPerformanceInterceptor)
                .saver(new MemorySaver())
                .build();
    }

}
