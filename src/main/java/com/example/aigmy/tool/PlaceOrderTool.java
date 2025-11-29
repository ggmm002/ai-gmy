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
public class PlaceOrderTool implements BiFunction<String, ToolContext,String> {


    @Override
    public String apply(@ToolParam(description = "客户信息和下单的汽车品牌车型信息") String orderInfo, ToolContext toolContext) {
        RunnableConfig runnableConfig = (RunnableConfig) toolContext.getContext().get("_AGENT_CONFIG_");
        Optional<Object> optional = runnableConfig.metadata("user_id");
        String userId = null;
        if(optional.isPresent()){
            userId = (String) optional.get();
        }
        if(userId == null){
            return "未获取到有效的用户信息";

        }
        return "用户id："+userId+"，下单成功";
    }
}
