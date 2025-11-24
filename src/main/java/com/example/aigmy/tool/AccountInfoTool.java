package com.example.aigmy.tool;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * @author guomaoyang 2025/11/23
 */
@Slf4j
public class AccountInfoTool implements BiFunction<String, ToolContext,String> {


  private static final Map<Long, Map<String, Object>> USER_DATABASE = Map.of(
    1L, Map.of(
        "name", "Alice Johnson",
        "account_type", "Premium",
        "balance", 5000,
        "email", "alice@example.com"
    ),
    2L, Map.of(
        "name", "Bob Smith",
        "account_type", "Standard",
        "balance", 1200,
        "email", "bob@example.com"
    )
);


    @Override
    public String apply(@ToolParam(required = false) String s, ToolContext toolContext) {
        RunnableConfig runnableConfig = (RunnableConfig) toolContext.getContext().get("_AGENT_CONFIG_");
        Optional<Object> optional = runnableConfig.metadata("user_id");
        Long userId = null;
        if(optional.isPresent()){
            userId = (Long) optional.get();
        }
        if(userId == null){
            return "未获取到有效的用户信息";

        }
        Map<String, Object> userInfo = USER_DATABASE.get(userId);
        if(userInfo == null){
            return "用户不存在";
        }
        return userInfo.toString();
    }
}
