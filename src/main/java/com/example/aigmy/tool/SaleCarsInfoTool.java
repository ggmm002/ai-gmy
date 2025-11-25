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
public class SaleCarsInfoTool implements BiFunction<String, ToolContext,String> {


  private static final Map<Long, Map<String, Object>> saleCarsInfo = Map.of(
    1L, Map.of(
        "name", "奔驰C260L",
        "price", 300000,
        "color", "白色",
        "inventory", 3
    ),
    2L, Map.of(
        "name", "宝马325Li",
        "price", 200000,
        "color", "黑色",
        "inventory", 0
    ),
    3L, Map.of(
        "name", "宝马330Li",
        "price", 250000,
        "color", "红色",
        "inventory", 5
    ),
    4L, Map.of(
        "name", "奥迪A4L",
        "price", 220000,
        "color", "蓝色",
        "inventory", 6
    ),
    5L, Map.of(
        "name", "奥迪A6L",
        "price", 280000,
        "color", "绿色",
        "inventory", 1
    )
);


    @Override
    public String apply(@ToolParam(required = false) String carName, ToolContext toolContext) {
        // RunnableConfig runnableConfig = (RunnableConfig) toolContext.getContext().get("_AGENT_CONFIG_");
        return saleCarsInfo.toString();
    }
}
