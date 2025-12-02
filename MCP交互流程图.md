# MCP 交互流程图（简化版）

## 一、npx 命令执行流程

```
用户配置
  │
  ├─> mcp-servers.json
  │     └─> {"command": "npx", "args": ["-y", "12306-mcp"]}
  │
  └─> Java 应用启动
        │
        ├─> ProcessBuilder
        │     └─> command: ["npx", "-y", "12306-mcp"]
        │
        └─> process.start()
              │
              └─> 操作系统创建进程
                    │
                    ├─> npx 进程启动
                    │     │
                    │     ├─> 检查本地是否有 12306-mcp
                    │     │     │
                    │     │     ├─> 有 → 直接执行
                    │     │     │
                    │     │     └─> 无 → 从 npm 下载
                    │     │           └─> 下载到临时目录
                    │     │
                    │     └─> 执行 12306-mcp 的主程序
                    │           │
                    │           └─> MCP 服务器进程运行
                    │                 │
                    │                 └─> 等待 STDIO 输入
                    │
                    └─> Java 获取进程的输入输出流
                          │
                          ├─> process.getOutputStream() → stdin（Java 写入）
                          │
                          └─> process.getInputStream()  → stdout（Java 读取）
```

## 二、MCP 初始化流程

```
┌─────────────────────────────────────────────────────────────┐
│                     初始化阶段                                │
└─────────────────────────────────────────────────────────────┘

时间 →  Java (McpClient)          MCP 服务器进程
        │                          │
        │  1. 启动进程              │
        ├─────────────────────────>│
        │  ProcessBuilder.start()  │
        │                          │
        │                          │  2. 进程启动
        │                          │     等待初始化消息
        │                          │
        │  3. 发送 initialize      │
        ├─────────────────────────>│
        │  {                        │
        │    "jsonrpc": "2.0",    │
        │    "id": 1,              │
        │    "method": "initialize",│
        │    "params": {           │
        │      "protocolVersion":  │
        │        "2024-11-05",    │
        │      "capabilities": {}, │
        │      "clientInfo": {    │
        │        "name": "ai-gmy" │
        │      }                   │
        │    }                     │
        │  }                        │
        │                          │
        │                          │  4. 处理 initialize
        │                          │     - 验证协议版本
        │                          │     - 准备工具列表
        │                          │     - 准备服务器信息
        │                          │
        │  5. 接收 initialize 响应 │
        │<─────────────────────────┤
        │  {                        │
        │    "jsonrpc": "2.0",    │
        │    "id": 1,              │
        │    "result": {           │
        │      "protocolVersion":  │
        │        "2024-11-05",    │
        │      "serverInfo": {    │
        │        "name": "12306-mcp"
        │      },                  │
        │      "tools": [         │
        │        {                 │
        │          "name": "search",
        │          "description":  │
        │            "搜索功能",   │
        │          "inputSchema": {...}
        │        }                 │
        │      ]                   │
        │    }                     │
        │  }                        │
        │                          │
        │  6. 解析工具列表          │
        │  7. 转换为 ToolCallback  │
        │                          │
        │  8. 发送 initialized     │
        ├─────────────────────────>│
        │  {                        │
        │    "jsonrpc": "2.0",    │
        │    "method":            │
        │      "notifications/initialized"
        │  }                        │
        │                          │
        │                          │  9. 握手完成
        │                          │     可以开始正常通信
        │                          │
        │  ✅ 初始化完成            │
        │                          │
```

## 三、工具调用流程

```
┌─────────────────────────────────────────────────────────────┐
│                     工具调用阶段                              │
└─────────────────────────────────────────────────────────────┘

时间 →  Java (McpClient)          MCP 服务器进程
        │                          │
        │  1. 用户请求工具调用      │
        │     callTool("search",   │
        │              "{\"query\": │
        │                \"新闻\"}")│
        │                          │
        │  2. 构建 JSON-RPC 请求   │
        │     {                    │
        │       "jsonrpc": "2.0", │
        │       "id": 2,          │
        │       "method":         │
        │         "tools/call",   │
        │       "params": {       │
        │         "name": "search",│
        │         "arguments": {  │
        │           "query": "新闻"│
        │         }                │
        │       }                  │
        │     }                    │
        │                          │
        │  3. 发送请求              │
        │     writer.println(json) │
        │     writer.flush()       │
        ├─────────────────────────>│
        │                          │
        │                          │  4. 接收请求
        │                          │     读取 stdin
        │                          │     解析 JSON
        │                          │
        │                          │  5. 执行工具
        │                          │     - 调用搜索函数
        │                          │     - 访问 API/数据库
        │                          │     - 处理结果
        │                          │
        │                          │  6. 构建响应
        │                          │     {                │
        │                          │       "jsonrpc": "2.0",
        │                          │       "id": 2,      │
        │                          │       "result": {   │
        │                          │         "content": [│
        │                          │           {         │
        │                          │             "type": │
        │                          │               "text",│
        │                          │             "text": │
        │                          │               "搜索结果..."
        │                          │           }         │
        │                          │         ]          │
        │                          │       }            │
        │                          │     }              │
        │                          │
        │  7. 接收响应              │
        │     reader.readLine()    │
        │<─────────────────────────┤
        │                          │
        │  8. 解析响应              │
        │     - 提取 result        │
        │     - 提取 content       │
        │     - 提取 text          │
        │                          │
        │  9. 返回结果给调用者     │
        │     return "搜索结果..."  │
        │                          │
```

## 四、消息监听机制

```
┌─────────────────────────────────────────────────────────────┐
│                   消息监听线程                                │
└─────────────────────────────────────────────────────────────┘

主线程                    监听线程                  MCP 服务器
  │                          │                        │
  │  1. 启动监听线程          │                        │
  ├─────────────────────────>│                        │
  │                          │                        │
  │                          │  2. 循环读取           │
  │                          │     reader.readLine()  │
  │                          ├───────────────────────>│
  │                          │                        │
  │                          │                        │  3. 写入响应
  │                          │                        │     stdout
  │                          │                        │
  │                          │  4. 接收消息           │
  │                          │<───────────────────────┤
  │                          │                        │
  │                          │  5. 解析 JSON          │
  │                          │     objectMapper       │
  │                          │                        │
  │                          │  6. 判断消息类型       │
  │                          │     │                  │
  │                          │     ├─> 有 id?         │
  │                          │     │   │              │
  │                          │     │   ├─> 是 → 响应  │
  │                          │     │   │     匹配 ID  │
  │                          │     │   │     完成 Future│
  │                          │     │   │              │
  │                          │     │   └─> 否 → 通知  │
  │                          │     │        处理通知  │
  │                          │     │                  │
  │                          │     └─> 继续循环       │
  │                          │                        │
  │  7. 等待 Future 完成     │                        │
  │     future.get()         │                        │
  │<──────────────────────────┤                        │
  │                          │                        │
```

## 五、STDIO 数据流

```
┌─────────────────────────────────────────────────────────────┐
│                    STDIO 数据流向                            │
└─────────────────────────────────────────────────────────────┘

Java 应用端                   操作系统                    MCP 服务器端
  │                              │                            │
  │  写入数据                    │                            │
  │  writer.println(json)       │                            │
  │  writer.flush()             │                            │
  ├─────────────────────────────>│                            │
  │                              │                            │
  │                              │  通过管道传输              │
  │                              ├───────────────────────────>│
  │                              │                            │
  │                              │                            │  读取数据
  │                              │                            │  stdin.read()
  │                              │                            │
  │                              │                            │  处理数据
  │                              │                            │  执行工具
  │                              │                            │
  │                              │                            │  写入响应
  │                              │                            │  stdout.write()
  │                              │                            │
  │                              │  通过管道传输              │
  │                              │<───────────────────────────┤
  │                              │                            │
  │  读取数据                    │                            │
  │  reader.readLine()          │                            │
  │<─────────────────────────────┤                            │
  │                              │                            │
```

## 六、完整生命周期

```
应用启动
  │
  ├─> 读取配置
  │     ├─> application.yml
  │     │     └─> spring.ai.mcp.client.enabled: true
  │     │
  │     └─> mcp-servers.json
  │           └─> {"command": "npx", "args": ["-y", "12306-mcp"]}
  │
  ├─> 自动配置类执行
  │     └─> McpClientAutoConfiguration
  │           │
  │           ├─> 创建 McpClient
  │           │     │
  │           │     ├─> 启动进程
  │           │     │     └─> ProcessBuilder.start()
  │           │     │
  │           │     ├─> 建立 STDIO 连接
  │           │     │     ├─> getOutputStream() → stdin
  │           │     │     └─> getInputStream()  → stdout
  │           │     │
  │           │     ├─> 启动消息监听线程
  │           │     │     └─> 持续读取响应
  │           │     │
  │           │     ├─> MCP 协议握手
  │           │     │     ├─> 发送 initialize
  │           │     │     ├─> 接收 initialize 响应
  │           │     │     └─> 发送 initialized
  │           │     │
  │           │     └─> 获取工具列表
  │           │           └─> 转换为 ToolCallback[]
  │           │
  │           └─> 创建 SyncMcpToolCallbackProvider
  │                 └─> 封装工具回调
  │
  ├─> 依赖注入
  │     └─> @Autowired SyncMcpToolCallbackProvider
  │           └─> 注入到 MyAgentConfiguration
  │
  ├─> 创建 ReactAgent
  │     └─> ReactAgent.builder()
  │           .tools(mcpTools)  ← 使用 MCP 工具
  │           .build()
  │
  ├─> 应用就绪
  │     │
  │     └─> 可以处理用户请求
  │           │
  │           ├─> 用户调用工具
  │           │     └─> ReactAgent.invoke()
  │           │
  │           ├─> Agent 决定调用工具
  │           │     └─> ToolCallback.call()
  │           │
  │           ├─> 转发到 MCP 客户端
  │           │     └─> mcpClient.callTool()
  │           │
  │           ├─> 发送 JSON-RPC 请求
  │           │     └─> 通过 STDIO
  │           │
  │           ├─> MCP 服务器执行
  │           │     └─> 返回结果
  │           │
  │           └─> 返回给 Agent
  │                 └─> 生成最终回答
  │
  └─> 应用关闭
        │
        ├─> 停止消息监听
        │
        ├─> 关闭 STDIO 连接
        │
        └─> 销毁进程
              └─> process.destroy()
```

## 七、关键数据结构

### 请求消息结构

```
JsonRpcRequest
├─ jsonrpc: "2.0"          (固定值)
├─ id: 1                   (请求 ID，用于匹配响应)
├─ method: "tools/call"     (方法名)
└─ params                   (参数对象)
    ├─ name: "search"       (工具名称)
    └─ arguments            (工具参数)
        └─ query: "新闻"    (具体参数)
```

### 响应消息结构

```
JsonRpcResponse
├─ jsonrpc: "2.0"          (固定值)
├─ id: 1                   (对应请求的 ID)
└─ result                   (结果对象，成功时)
    └─ content              (内容数组)
        └─ [0]              (第一个内容项)
            ├─ type: "text" (内容类型)
            └─ text: "..."  (文本内容)
```

### 错误响应结构

```
JsonRpcErrorResponse
├─ jsonrpc: "2.0"          (固定值)
├─ id: 1                   (对应请求的 ID)
└─ error                    (错误对象)
    ├─ code: -32603         (错误代码)
    ├─ message: "..."       (错误消息)
    └─ data                  (错误详情，可选)
```

## 八、关键代码片段

### 发送请求

```java
// 1. 创建请求对象
ObjectNode request = objectMapper.createObjectNode();
request.put("jsonrpc", "2.0");
request.put("id", requestId.getAndIncrement());
request.put("method", "tools/call");

// 2. 设置参数
ObjectNode params = objectMapper.createObjectNode();
params.put("name", toolName);
params.set("arguments", parseArguments(argumentsJson));
request.set("params", params);

// 3. 序列化为 JSON 字符串
String json = objectMapper.writeValueAsString(request);

// 4. 写入到进程的标准输入
writer.println(json);
writer.flush();
```

### 接收响应

```java
// 1. 从进程的标准输出读取一行
String line = reader.readLine();

// 2. 反序列化为 JSON 对象
JsonNode response = objectMapper.readTree(line);

// 3. 检查是否有错误
if (response.has("error")) {
    throw new McpException(response.get("error"));
}

// 4. 提取结果
JsonNode result = response.get("result");
JsonNode content = result.get("content");

// 5. 提取文本内容
if (content.isArray() && content.size() > 0) {
    JsonNode firstItem = content.get(0);
    if (firstItem.has("text")) {
        return firstItem.get("text").asText();
    }
}
```

## 总结

**npx 的作用**：
- 自动下载并执行 npm 包
- 启动 MCP 服务器进程
- 无需手动安装依赖

**交互过程**：
1. 启动进程 → 2. 建立 STDIO 连接 → 3. 协议握手 → 4. 获取工具 → 5. 工具调用 → 6. 接收结果

**关键技术**：
- STDIO 进程间通信
- JSON-RPC 2.0 协议
- 请求 ID 匹配机制
- 异步消息处理

整个过程实现了 Java 应用与 Node.js MCP 服务器的无缝集成！
