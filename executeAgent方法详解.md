# executeAgent 方法执行逻辑详解

## 方法概述

这是一个处理 **Agent 流式响应**的方法，将 Agent 的输出转换为 **SSE (Server-Sent Events)** 格式，实现实时推送给前端。

### 方法签名

```java
private Flux<ServerSentEvent<String>> executeAgent(
    UserMessage userMessage,    // 用户消息
    BaseAgent agent,            // AI 智能体
    RunnableConfig runnableConfig // 运行配置（包含会话ID等）
)
```

**返回值**: `Flux<ServerSentEvent<String>>` - 响应式流，不断推送 SSE 事件

## 核心问题：为什么这么复杂？

### 背景问题

在 **stream 模式**下，Agent 存在一个问题：
- Agent 检测到需要调用工具时
- Stream 会**直接结束**（返回 `__END__` 节点）
- **工具不会自动执行**

### 解决方案

检测到工具调用后，使用 `invoke()` 方法继续执行工具。

## 执行流程图

```
开始
  │
  ├─> 1. 启动 Agent Stream
  │     └─> agent.stream(userMessage, runnableConfig)
  │
  ├─> 2. 订阅流事件（监听数据流）
  │     │
  │     ├─> doOnSubscribe: 记录订阅日志
  │     │
  │     ├─> doOnNext: 处理每个输出节点
  │     │     │
  │     │     └─> 检测是否有工具调用
  │     │           └─> hasToolCalls = true
  │     │
  │     ├─> doOnCancel: 处理取消事件
  │     │
  │     ├─> doOnComplete: 处理完成事件
  │     │
  │     └─> doOnError: 处理错误事件
  │
  ├─> 3. 转换每个节点
  │     │
  │     └─> concatMap: 逐个处理节点
  │           │
  │           ├─> 如果是 __END__ 节点 且 有工具调用
  │           │     │
  │           │     ├─> 返回 __END__ 事件
  │           │     │
  │           │     └─> 使用 invoke() 继续执行工具
  │           │           └─> 返回工具执行结果
  │           │
  │           └─> 否则：正常转换为 SSE 事件
  │
  ├─> 4. 错误处理
  │     │
  │     └─> onErrorResume: 捕获错误，转换为错误事件
  │
  └─> 5. 返回 SSE 流给前端
```

## 详细步骤解析

### 步骤 1: 启动 Agent Stream

```java
Flux<NodeOutput> agentStream;

if (userMessage != null) {
    log.info("使用 UserMessage 启动 stream，内容: {}", userMessage.getText());
    agentStream = agent.stream(userMessage, runnableConfig);
} else {
    log.info("使用空字符串启动 stream");
    agentStream = agent.stream("", runnableConfig);
}
```

**作用**: 
- 启动 Agent 的流式处理
- 返回 `Flux<NodeOutput>` 响应式流
- NodeOutput 是 Agent 处理过程中的每个输出节点

### 步骤 2: 监听流事件

#### 2.1 订阅监听

```java
.doOnSubscribe(subscription -> {
    log.info("Agent stream 已订阅");
})
```

**作用**: 当前端开始订阅这个流时触发，记录日志。

#### 2.2 处理每个节点 (核心逻辑)

```java
.doOnNext(nodeOutput -> {
    log.info("收到 NodeOutput，类型: {}, node: {}, agent: {}", 
            nodeOutput.getClass().getSimpleName(), 
            nodeOutput.node(), 
            nodeOutput.agent());
    
    // 检测工具调用
    if (nodeOutput instanceof StreamingOutput<?> streamingOutput) {
        Message msg = streamingOutput.message();
        if (msg instanceof AssistantMessage assistantMessage && 
            assistantMessage.hasToolCalls()) {
            hasToolCalls.set(true);  // 标记：检测到工具调用
            log.warn("检测到工具调用，标记需要继续执行");
        }
    }
})
```

**作用**:
1. **记录日志**: 输出节点类型、名称等信息
2. **检测工具调用**: 
   - 检查是否是 `StreamingOutput` 类型
   - 检查消息是否包含工具调用 (`hasToolCalls()`)
   - 如果有，设置标记 `hasToolCalls = true`

**为什么需要标记？**
- 因为工具调用后，stream 会直接结束
- 需要在后续步骤中使用 `invoke()` 继续执行

#### 2.3 其他监听

```java
.doOnCancel(() -> log.warn("Agent stream 被取消"))
.doOnComplete(() -> log.info("Agent stream 执行完成"))
.doOnError(error -> log.error("Agent stream 执行出错"))
```

### 步骤 3: 转换节点为 SSE 事件 (核心处理)

```java
.concatMap(nodeOutput -> {
    // 检查：是 __END__ 节点 且 之前检测到工具调用
    if ("__END__".equals(nodeOutput.node()) && hasToolCalls.get()) {
        log.warn("检测到工具调用但stream已结束，使用invoke继续执行工具");
        
        // 3.1 先返回 __END__ 事件
        Flux<ServerSentEvent<String>> endEvent = Flux.just(convertToSSE(nodeOutput));
        
        // 3.2 然后使用 invoke 继续执行工具
        Flux<ServerSentEvent<String>> invokeResult = Flux.defer(() -> {
            try {
                log.info("使用invoke继续执行，threadId: {}", runnableConfig.threadId());
                
                // 调用 invoke() 执行工具
                Optional<OverAllState> result = agent.invoke("", runnableConfig);
                
                if (result.isPresent()) {
                    log.info("invoke执行完成，结果: {}", JSON.toJSONString(result.get()));
                    
                    // 将结果转换为 SSE 事件
                    String resultText = result.get().toString();
                    String jsonData = String.format(
                        "{\"node\":\"__INVOKE_RESULT\",\"agent\":\"%s\",\"text\":%s}",
                        agent.getClass().getSimpleName(),
                        mapper.writeValueAsString(resultText)
                    );
                    
                    return Flux.just(ServerSentEvent.<String>builder()
                        .data(jsonData)
                        .build());
                }
                return Flux.empty();
            } catch (Exception e) {
                log.error("invoke继续执行失败", e);
                return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"error\":\"invoke执行失败: " + e.getMessage() + "\"}")
                    .build());
            }
        });
        
        // 3.3 合并两个流：先返回 __END__，再返回工具执行结果
        return endEvent.concatWith(invokeResult);
    }
    
    // 正常情况：直接转换为 SSE 事件
    return Flux.just(convertToSSE(nodeOutput));
})
```

**执行逻辑**:

1. **检查条件**: `__END__` 节点 + 有工具调用
   - 满足条件 → 执行工具调用逻辑
   - 不满足 → 正常转换为 SSE

2. **工具调用处理**:
   ```
   返回 __END__ 事件
      ↓
   调用 invoke() 执行工具
      ↓
   获取工具执行结果
      ↓
   转换为 SSE 事件
      ↓
   返回给前端
   ```

3. **使用 `concatWith`**:
   - 先推送 `__END__` 事件
   - 再推送工具执行结果
   - 保证顺序

### 步骤 4: 处理空流情况

```java
.switchIfEmpty(Flux.defer(() -> {
    // 如果整个 stream 为空，也检查是否需要 invoke
    if (hasToolCalls.get()) {
        log.warn("Stream为空但检测到工具调用，使用invoke执行");
        try {
            Optional<OverAllState> result = agent.invoke("", runnableConfig);
            if (result.isPresent()) {
                String resultText = result.get().toString();
                String jsonData = String.format(
                    "{\"node\":\"__INVOKE_RESULT\",\"agent\":\"%s\",\"text\":%s}",
                    agent.getClass().getSimpleName(),
                    mapper.writeValueAsString(resultText)
                );
                return Flux.just(ServerSentEvent.<String>builder()
                    .data(jsonData)
                    .build());
            }
        } catch (Exception e) {
            log.error("invoke执行失败", e);
        }
    }
    return Flux.empty();
}))
```

**作用**: 
- 如果 stream 完全为空（没有任何输出）
- 但检测到了工具调用
- 仍然尝试使用 `invoke()` 执行

### 步骤 5: 错误处理

```java
.onErrorResume(error -> {
    log.error("Error occurred during agent stream execution", error);
    
    // 创建错误响应
    String errorMessage = error.getMessage() != null ? 
        error.getMessage() : "Unknown error occurred";
    String errorType = error.getClass().getSimpleName();
    
    try {
        String errorJson = String.format(
            "{\"error\":true,\"errorType\":\"%s\",\"errorMessage\":\"%s\"}",
            errorType.replace("\"", "\\\""),
            errorMessage.replace("\"", "\\\"").replace("\n", "\\n")
        );
        
        // 返回错误事件，不中断流
        return Flux.just(
            ServerSentEvent.<String>builder()
                .event("error")
                .data(errorJson)
                .build()
        );
    } catch (Exception e) {
        log.error("Failed to create error SSE event", e);
        return Flux.just(
            ServerSentEvent.<String>builder()
                .event("error")
                .data("{\"error\":true,\"errorMessage\":\"Internal error occurred\"}")
                .build()
        );
    }
})
```

**作用**:
- 捕获流处理中的任何错误
- 将错误转换为 SSE 错误事件
- 返回给前端，而不是直接中断连接

## 实际执行示例

### 场景：用户查询天气

```
用户输入: "查询北京的天气"
  │
  └─> 1. 启动 stream
        agent.stream("查询北京的天气", config)
          │
          ├─> NodeOutput 1: __START__ 节点
          │     └─> 转换为 SSE → 推送给前端
          │
          ├─> NodeOutput 2: StreamingOutput (包含工具调用)
          │     ├─> 检测到工具调用 → hasToolCalls = true
          │     └─> 转换为 SSE → 推送给前端
          │
          ├─> NodeOutput 3: __END__ 节点
          │     ├─> 检查：是 __END__ 且 hasToolCalls = true
          │     │
          │     ├─> 返回 __END__ 事件 → 推送给前端
          │     │
          │     └─> 调用 invoke("", config)
          │           │
          │           ├─> Agent 执行工具调用
          │           │     └─> 调用 get_weather("北京")
          │           │           └─> 返回 "北京市是晴天"
          │           │
          │           ├─> 获取结果: "北京市是晴天"
          │           │
          │           └─> 转换为 SSE 事件 → 推送给前端
          │
          └─> 完成
```

### 前端收到的事件序列

```javascript
// 事件 1: __START__
data: {"node":"__START__","agent":"ReactAgent"}

// 事件 2: 工具调用检测
data: {"node":"agent","agent":"ReactAgent","toolCalls":[...]}

// 事件 3: __END__
data: {"node":"__END__","agent":"ReactAgent"}

// 事件 4: 工具执行结果
data: {"node":"__INVOKE_RESULT","agent":"ReactAgent","text":"北京市是晴天"}
```

## 关键技术点

### 1. Reactor 响应式编程

```java
Flux<ServerSentEvent<String>>  // 响应式流
  .doOnNext()     // 处理每个元素（不改变流）
  .concatMap()    // 转换每个元素（可以返回流）
  .switchIfEmpty() // 处理空流
  .onErrorResume() // 错误恢复
```

### 2. 为什么使用 `AtomicBoolean`？

```java
final AtomicBoolean hasToolCalls = new AtomicBoolean(false);
```

- **线程安全**: 在响应式流中可能被多个线程访问
- **共享状态**: 在多个操作符之间共享标记

### 3. 为什么使用 `Flux.defer()`？

```java
Flux.defer(() -> {
    // 延迟执行
})
```

- **延迟执行**: 只有在需要时才执行 `invoke()`
- **避免阻塞**: 不会阻塞流的创建过程

### 4. 为什么使用 `concatWith()`？

```java
endEvent.concatWith(invokeResult)
```

- **保证顺序**: 先推送 `__END__`，再推送结果
- **合并流**: 将两个流合并为一个

## 与普通方法的对比

### 普通同步方法

```java
public String chat(String question) {
    // 1. 调用 agent
    OverAllState result = agent.invoke(question);
    
    // 2. 返回结果
    return result.toString();
}
```

**问题**:
- 用户需要等待全部处理完成
- 无法实时看到处理进度
- 超时时间长

### 流式方法（当前实现）

```java
public Flux<ServerSentEvent<String>> search(String question) {
    // 返回响应式流
    return executeAgent(userMessage, agent, config);
}
```

**优势**:
- ✅ 实时推送处理进度
- ✅ 用户体验更好
- ✅ 可以显示 "正在思考..."、"正在调用工具..." 等状态
- ✅ 支持取消操作

## 总结

这个方法的核心逻辑：

1. **启动 Agent Stream** → 获取响应式流
2. **监听流事件** → 检测工具调用
3. **转换为 SSE** → 推送给前端
4. **特殊处理** → 工具调用时使用 `invoke()` 继续执行
5. **错误处理** → 转换为错误事件，不中断连接

**为什么复杂？**
- 因为要解决 stream 模式下工具不自动执行的问题
- 因为要提供实时的流式响应
- 因为要处理各种异常情况

**核心价值**:
实现了**实时、流式、可靠**的 AI 交互体验！
