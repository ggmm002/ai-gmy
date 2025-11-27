package com.example.aigmy.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.example.aigmy.interceptor.ContentInterceptor;
import com.example.aigmy.interceptor.ModelPerformanceInterceptor;
import com.example.aigmy.tool.*;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

import java.util.List;

/**
 * @author guomaoyang 2025/11/22
 */
@Configuration
public class MyAgentConfiguration {

    private static final String SYSTEM_PROMPT = """
    你是一个汽车门店的销售经理，你的任务是根据用户的问题，给出相应的回答。
    客户询问销售的车型，你可以使用saleCarsTool工具查询。客户询问的车型信息，你可以使用saleCarsInfoTool工具查询，
    当客户询问的车型名称不完全与工具查询的一致，你需要从查询结果中提取车型名称，然后与客户确认。
    其他需要传入‘车型名称’参数的工具，你需要完全按照从工具查询结果中提取的车型名称传入。
    用户的问题可能是：
    1. 你们店有哪些汽车品牌？
    2. 你们店有哪些汽车型号？
    3. 你们店有哪些汽车价格？
    4. 你们店有哪些汽车颜色？
    5. 你们店有哪些汽车配置？
    6. 你们店有哪些汽车优惠政策？
    """;
    
    private static final String SYSTEM_VL_PROMPT = """
      你是一个专业的视觉理解模型，实现思考模式和非思考模式的有效融合，通过图像理解、图像生成、图像编辑、图像分析等能力，实现对图像的深度理解。
      你的任务是根据用户的问题，给出相应的回答。
      """;

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Autowired
    private ContentInterceptor contentInterceptor;

    @Autowired
    private ModelPerformanceInterceptor modelPerformanceInterceptor;

    private ToolCallback accountInfoTool;
    private ToolCallback carBrandTool;
    private ToolCallback placeOrderTool;

    private ToolCallback saleCarsInfoTool;

    @PostConstruct
    public void init() {
        this.accountInfoTool = FunctionToolCallback.builder("accountInfoTool", new AccountInfoTool())
                .description("查询当前用户的账号信息")
                .inputType(String.class)
                .build();
        this.carBrandTool = FunctionToolCallback.builder("carBrandTool", new CarBrandTool())
                .description("查询销售的汽车品牌")
                .build();
        this.placeOrderTool = FunctionToolCallback.builder("placeOrderTool", new PlaceOrderTool())
                .description("下单操作")
                .inputType(String.class)
                .build();
        this.saleCarsInfoTool = FunctionToolCallback.builder("saleCarsInfoTool", new SaleCarsInfoTool())
                .description("查询销售的车型信息")
                .inputType(String.class)
                .build();
    }



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

    @Bean("vlAgent")
    public ReactAgent vlAgent(){
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


        return ReactAgent.builder()
                .name("vlAgent")
                .model(chatModel)
                .systemPrompt(SYSTEM_VL_PROMPT)
                .saver(new MemorySaver())
                .build();
    }

    @Bean("hitlAgent")
    public ReactAgent hitlAgent(){
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(apiKey)
                .build();

        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen3-max")
                        .build())
                .build();

        HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
                .approvalOn("placeOrderTool", ToolConfig.builder()
                        .description("下单操作需要人工审批")
                        .build())
                .build();


        return ReactAgent.builder()
                .name("hitlAgent")
                .model(chatModel)
                .systemPrompt(SYSTEM_PROMPT)
                .tools(accountInfoTool,placeOrderTool,saleCarsInfoTool)
                .hooks(List.of(humanInTheLoopHook))
                .saver(new MemorySaver())
                .build();

    }

    @Bean("multiAgent")
    public ReactAgent multiAgent() {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(apiKey)
                .build();

        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen3-max")
                        .build())
                .build();

        ReactAgent writerAgent = ReactAgent.builder()
                .name("full_typed_writer")
                .model(chatModel)
                .description("完整类型化的写作工具")
                .instruction("根据结构化输入（topic、wordCount、style）创作文章，并返回结构化输出（title、content、characterCount）。")
                .inputType(ArticleRequest.class) // [!code highlight]
                .outputType(ArticleOutput.class) // [!code highlight]
                .build();

        ReactAgent reviewerAgent = ReactAgent.builder()
                .name("typed_reviewer")
                .model(chatModel)
                .description("完整类型化的评审工具")
                .instruction("对文章进行评审，返回评审意见（comment、approved、suggestions）。")
                .outputType(ReviewOutput.class) // [!code highlight]
                .build();
        return ReactAgent.builder()
                .name("orchestrator")
                .model(chatModel)
                .instruction("协调写作和评审流程。先调用写作工具创作文章，然后调用评审工具进行评审。")
                .tools(

                        AgentTool.getFunctionToolCallback(writerAgent),
                        AgentTool.getFunctionToolCallback(reviewerAgent)
                )
                .build();
    }

}
class ArticleRequest{
    private String topic;
    private int wordCount;
    private String style;

}

 class ArticleOutput {
    private String title;
    private String content;
    private int characterCount;
    // getters and setters
}

 class ReviewOutput {
    private String comment;
    private boolean approved;
    private List<String> suggestions;
    // getters and setters
}
