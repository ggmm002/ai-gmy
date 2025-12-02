# MCP 进程交互详解

## 一、npx 命令的作用

### 1.1 npx 是什么？

`npx` 是 Node.js 自带的包执行工具（Node Package eXecute），用于：

1. **执行 npm 包中的可执行文件**
   - 无需全局安装包
   - 临时下载并执行
   - 执行完后可以清理

2. **自动查找和执行命令**
   - 在 `node_modules/.bin` 中查找
   - 在全局包中查找
   - 如果找不到，从 npm 仓库下载

### 1.2 npx 命令解析

在你的配置中：
```json
{
  "command": "npx",
  "args": ["-y", "12306-mcp"]
}
```

**命令分解**：
```bash
npx -y 12306-mcp
```

- `npx`: 执行命令
- `-y`: 自动确认（yes），跳过确认提示
- `12306-mcp`: npm 包名

**执行过程**：
1. npx 检查本地是否已安装 `12306-mcp`
2. 如果未安装，从 npm 仓库下载（首次运行）
3. 执行 `12306-mcp` 包中的主程序
4. 该程序作为 MCP 服务器运行

### 1.3 为什么使用 npx？

**优势**：
- ✅ **无需手动安装**: 不需要先 `npm install -g 12306-mcp`
- ✅ **版本管理**: 自动使用最新版本（或指定版本）
- ✅ **环境隔离**: 每个项目可以使用不同版本
- ✅ **简化部署**: 配置文件中指定即可，无需额外安装步骤

**对比**：
```bash
# 传统方式（需要手动安装）
npm install -g 12306-mcp
12306-mcp  # 直接执行

# npx 方式（自动处理）
npx -y 12306-mcp  # 自动下载并执行
```

### 1.4 npx 执行后的进程

当执行 `npx -y 12306-mcp` 时：

```
父进程（Java 应用）
  └─> 启动子进程
       └─> npx 进程
            └─> 下载/查找 12306-mcp 包
                 └─> 执行 12306-mcp 的主程序
                      └─> MCP 服务器进程运行
```

**进程关系**：
- Java 应用是父进程
- npx 是中间进程（可能很快退出）
- MCP 服务器是最终的子进程

## 二、McpClient 与 MCP 进程交互详解

### 2.1 整体架构

```
┌─────────────────┐
│  Java 应用      │
│  McpClient      │
└────────┬────────┘
         │ STDIO (标准输入输出)
         │
         ▼
┌─────────────────┐
│  MCP 服务器进程 │
│  (12306-mcp)    │
└─────────────────┘
```

**通信方式**：STDIO (Standard Input/Output)
- **标准输入 (stdin)**: Java → MCP 服务器（发送请求）
- **标准输出 (stdout)**: MCP 服务器 → Java（接收响应）
- **标准错误 (stderr)**: MCP 服务器日志（可选）

### 2.2 进程启动过程

#### 步骤 1: 创建进程构建器

```java
ProcessBuilder processBuilder = new ProcessBuilder();
processBuilder.command("npx", "-y", "12306-mcp");

// 设置工作目录（可选）
processBuilder.directory(new File("/path/to/workdir"));

// 设置环境变量（可选）
Map<String, String> env = processBuilder.environment();
env.put("NODE_ENV", "production");
```

#### 步骤 2: 配置输入输出重定向

```java
// 重定向标准错误到标准输出（合并日志）
processBuilder.redirectErrorStream(true);

// 或者分别处理
processBuilder.redirectError(new File("mcp-error.log"));
processBuilder.redirectOutput(new File("mcp-output.log"));
```

#### 步骤 3: 启动进程

```java
Process process = processBuilder.start();

// 获取进程的输入输出流
OutputStream stdin = process.getOutputStream();  // 写入到进程（发送数据）
InputStream stdout = process.getInputStream();   // 从进程读取（接收数据）
InputStream stderr = process.getErrorStream();   // 错误流（日志）
```

**关键点**：
- `getOutputStream()`: 返回的是**写入到进程的流**（Java 写入，MCP 读取）
- `getInputStream()`: 返回的是**从进程读取的流**（MCP 写入，Java 读取）

### 2.3 STDIO 通信机制

#### 数据流向

```
Java 应用写入数据          MCP 服务器读取数据
     │                          │
     │  "Hello\n"               │
     ├─────────────────────────>│
     │                          │
     │                          │ 处理数据
     │                          │
     │                          │ 写入响应
     │                          │
     │  "World\n"               │
     │<─────────────────────────┤
     │                          │
Java 应用读取数据          MCP 服务器写入数据
```

#### 实际代码示例

```java
public class StdioTransport {
    private final OutputStream stdin;  // 写入到 MCP 服务器
    private final InputStream stdout;  // 从 MCP 服务器读取
    private final BufferedReader reader;
    private final PrintWriter writer;
    
    public StdioTransport(Process process) {
        this.stdin = process.getOutputStream();
        this.stdout = process.getInputStream();
        
        // 使用缓冲流提高性能
        this.reader = new BufferedReader(
            new InputStreamReader(stdout, StandardCharsets.UTF_8)
        );
        this.writer = new PrintWriter(
            new OutputStreamWriter(stdin, StandardCharsets.UTF_8),
            true  // 自动刷新
        );
    }
    
    // 发送消息到 MCP 服务器
    public void send(Object message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        writer.println(json);  // 写入一行 JSON
        writer.flush();        // 确保立即发送
    }
    
    // 从 MCP 服务器接收消息
    public String receive() throws IOException {
        String line = reader.readLine();  // 读取一行 JSON
        if (line == null) {
            throw new IOException("MCP 服务器连接已关闭");
        }
        return line;
    }
}
```

### 2.4 MCP 协议消息格式

MCP 使用 **JSON-RPC 2.0** 协议，消息格式：

#### 请求消息（Request）

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {
      "tools": {}
    },
    "clientInfo": {
      "name": "ai-gmy-mcp-client",
      "version": "1.0.0"
    }
  }
}
```

**字段说明**：
- `jsonrpc`: 协议版本（固定为 "2.0"）
- `id`: 请求 ID（用于匹配请求和响应）
- `method`: 方法名（如 "initialize", "tools/call"）
- `params`: 方法参数

#### 响应消息（Response）

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2024-11-05",
    "capabilities": {
      "tools": {
        "listChanged": true
      }
    },
    "serverInfo": {
      "name": "12306-mcp",
      "version": "1.0.0"
    },
    "tools": [
      {
        "name": "search",
        "description": "搜索功能",
        "inputSchema": {
          "type": "object",
          "properties": {
            "query": {
              "type": "string",
              "description": "搜索关键词"
            }
          },
          "required": ["query"]
        }
      }
    ]
  }
}
```

**字段说明**：
- `jsonrpc`: 协议版本
- `id`: 对应请求的 ID
- `result`: 响应结果（成功时）
- `error`: 错误信息（失败时）

#### 通知消息（Notification）

```json
{
  "jsonrpc": "2.0",
  "method": "notifications/initialized"
}
```

**特点**：没有 `id` 字段，不需要响应

### 2.5 完整的交互流程

#### 阶段 1: 初始化握手

```
┌─────────────┐                    ┌─────────────┐
│ McpClient   │                    │ MCP 服务器  │
└──────┬──────┘                    └──────┬──────┘
       │                                  │
       │  1. initialize 请求              │
       ├─────────────────────────────────>│
       │  {                                │
       │    "method": "initialize",       │
       │    "params": {...}               │
       │  }                                │
       │                                  │
       │                                  │  2. 处理请求
       │                                  │     解析参数
       │                                  │     准备响应
       │                                  │
       │  3. initialize 响应              │
       │<─────────────────────────────────┤
       │  {                                │
       │    "id": 1,                      │
       │    "result": {                   │
       │      "tools": [...]             │
       │    }                             │
       │  }                                │
       │                                  │
       │  4. initialized 通知             │
       ├─────────────────────────────────>│
       │  {                                │
       │    "method": "notifications/initialized"
       │  }                                │
       │                                  │
       │                                  │  5. 握手完成
       │                                  │     可以开始正常通信
       │                                  │
```

**代码实现**：

```java
public class McpClient {
    private StdioTransport transport;
    private AtomicInteger requestId = new AtomicInteger(1);
    private Map<Integer, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();
    
    public void initialize() throws IOException {
        // 1. 发送 initialize 请求
        int id = requestId.getAndIncrement();
        JsonNode request = createInitializeRequest(id);
        transport.send(request);
        
        // 2. 等待响应
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingRequests.put(id, future);
        
        // 3. 启动消息监听线程
        startMessageListener();
        
        // 4. 等待 initialize 响应
        JsonNode response = future.get(30, TimeUnit.SECONDS);
        
        // 5. 解析工具列表
        JsonNode tools = response.get("result").get("tools");
        this.tools = parseTools(tools);
        
        // 6. 发送 initialized 通知
        sendNotification("notifications/initialized", null);
        
        log.info("MCP 初始化完成，获取到 {} 个工具", this.tools.size());
    }
    
    private JsonNode createInitializeRequest(int id) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", "initialize");
        
        ObjectNode params = objectMapper.createObjectNode();
        params.put("protocolVersion", "2024-11-05");
        
        ObjectNode capabilities = objectMapper.createObjectNode();
        params.set("capabilities", capabilities);
        
        ObjectNode clientInfo = objectMapper.createObjectNode();
        clientInfo.put("name", "ai-gmy-mcp-client");
        clientInfo.put("version", "1.0.0");
        params.set("clientInfo", clientInfo);
        
        request.set("params", params);
        return request;
    }
}
```

#### 阶段 2: 消息监听

```java
private void startMessageListener() {
    Thread listenerThread = new Thread(() -> {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // 读取一行 JSON 消息
                String line = transport.receive();
                
                if (line == null) {
                    break;  // 连接关闭
                }
                
                // 解析 JSON
                JsonNode message = objectMapper.readTree(line);
                
                // 处理消息
                handleMessage(message);
            }
        } catch (IOException e) {
            log.error("消息监听线程异常", e);
        }
    }, "mcp-message-listener");
    
    listenerThread.setDaemon(true);
    listenerThread.start();
}

private void handleMessage(JsonNode message) {
    // 检查是否是响应（有 id 字段）
    if (message.has("id")) {
        int id = message.get("id").asInt();
        CompletableFuture<JsonNode> future = pendingRequests.remove(id);
        
        if (future != null) {
            // 完成对应的 Future
            if (message.has("result")) {
                future.complete(message.get("result"));
            } else if (message.has("error")) {
                future.completeExceptionally(
                    new McpException(message.get("error"))
                );
            }
        }
    } 
    // 检查是否是通知（无 id 字段）
    else if (message.has("method")) {
        String method = message.get("method").asText();
        handleNotification(method, message.get("params"));
    }
}
```

#### 阶段 3: 工具调用

```
┌─────────────┐                    ┌─────────────┐
│ McpClient   │                    │ MCP 服务器  │
└──────┬──────┘                    └──────┬──────┘
       │                                  │
       │  1. tools/call 请求              │
       ├─────────────────────────────────>│
       │  {                                │
       │    "method": "tools/call",      │
       │    "params": {                   │
       │      "name": "search",          │
       │      "arguments": {             │
       │        "query": "今天的新闻"    │
       │      }                           │
       │    }                             │
       │  }                                │
       │                                  │
       │                                  │  2. 执行工具
       │                                  │     调用搜索功能
       │                                  │     获取结果
       │                                  │
       │  3. tools/call 响应              │
       │<─────────────────────────────────┤
       │  {                                │
       │    "id": 2,                      │
       │    "result": {                   │
       │      "content": [               │
       │        {                         │
       │          "type": "text",        │
       │          "text": "搜索结果..."  │
       │        }                         │
       │      ]                           │
       │    }                             │
       │  }                                │
       │                                  │
```

**代码实现**：

```java
public String callTool(String toolName, String arguments) throws IOException {
    // 1. 创建请求
    int id = requestId.getAndIncrement();
    ObjectNode request = objectMapper.createObjectNode();
    request.put("jsonrpc", "2.0");
    request.put("id", id);
    request.put("method", "tools/call");
    
    ObjectNode params = objectMapper.createObjectNode();
    params.put("name", toolName);
    
    // 解析参数 JSON 字符串
    JsonNode argumentsNode = objectMapper.readTree(arguments);
    params.set("arguments", argumentsNode);
    
    request.set("params", params);
    
    // 2. 发送请求
    CompletableFuture<JsonNode> future = new CompletableFuture<>();
    pendingRequests.put(id, future);
    
    transport.send(request);
    
    // 3. 等待响应（带超时）
    try {
        JsonNode response = future.get(20, TimeUnit.SECONDS);
        
        // 4. 提取结果
        JsonNode content = response.get("content");
        if (content.isArray() && content.size() > 0) {
            JsonNode firstContent = content.get(0);
            if (firstContent.has("text")) {
                return firstContent.get("text").asText();
            }
        }
        
        return response.toString();
    } catch (TimeoutException e) {
        pendingRequests.remove(id);
        throw new IOException("工具调用超时", e);
    }
}
```

### 2.6 错误处理

#### 错误响应格式

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "error": {
    "code": -32603,
    "message": "Internal error",
    "data": {
      "details": "具体错误信息"
    }
  }
}
```

**错误代码**：
- `-32700`: Parse error（解析错误）
- `-32600`: Invalid Request（无效请求）
- `-32601`: Method not found（方法不存在）
- `-32602`: Invalid params（参数无效）
- `-32603`: Internal error（内部错误）
- `-32000` to `-32099`: Server error（服务器错误）

#### 错误处理代码

```java
private void handleMessage(JsonNode message) {
    if (message.has("error")) {
        JsonNode error = message.get("error");
        int code = error.get("code").asInt();
        String messageText = error.get("message").asText();
        
        int id = message.get("id").asInt();
        CompletableFuture<JsonNode> future = pendingRequests.remove(id);
        
        if (future != null) {
            future.completeExceptionally(
                new McpException(code, messageText)
            );
        }
    }
}
```

### 2.7 进程生命周期管理

#### 启动

```java
public void start() throws IOException {
    ProcessBuilder pb = new ProcessBuilder("npx", "-y", "12306-mcp");
    pb.redirectErrorStream(true);
    
    this.process = pb.start();
    this.transport = new StdioTransport(process);
    
    // 等待进程就绪
    Thread.sleep(1000);
    
    // 初始化协议
    initialize();
}
```

#### 关闭

```java
public void shutdown() {
    try {
        // 1. 停止消息监听
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        
        // 2. 关闭传输层
        if (transport != null) {
            transport.close();
        }
        
        // 3. 销毁进程
        if (process != null && process.isAlive()) {
            // 先尝试正常关闭
            process.destroy();
            
            // 等待 5 秒
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                // 强制终止
                process.destroyForcibly();
            }
        }
    } catch (Exception e) {
        log.error("关闭 MCP 客户端失败", e);
    }
}
```

#### 健康检查

```java
public boolean isAlive() {
    return process != null && process.isAlive();
}

public void checkHealth() throws IOException {
    if (!isAlive()) {
        throw new IOException("MCP 服务器进程已停止");
    }
    
    // 可以发送一个 ping 请求测试连接
    // 这里简化处理
}
```

### 2.8 完整交互时序图

```
时间轴 →
┌─────────────┐                    ┌─────────────┐
│ McpClient   │                    │ MCP 服务器  │
└──────┬──────┘                    └──────┬──────┘
       │                                  │
       │  1. 启动进程                     │
       │  ProcessBuilder.start()          │
       ├─────────────────────────────────>│
       │                                  │
       │                                  │  2. 进程启动
       │                                  │     等待初始化
       │                                  │
       │  3. initialize 请求              │
       ├─────────────────────────────────>│
       │                                  │
       │                                  │  4. 处理初始化
       │                                  │     准备工具列表
       │                                  │
       │  5. initialize 响应              │
       │<─────────────────────────────────┤
       │                                  │
       │  6. 解析工具列表                  │
       │  7. initialized 通知            │
       ├─────────────────────────────────>│
       │                                  │
       │                                  │  8. 握手完成
       │                                  │
       │  [正常通信阶段]                   │
       │                                  │
       │  9. tools/call 请求              │
       ├─────────────────────────────────>│
       │                                  │
       │                                  │  10. 执行工具
       │                                  │      调用搜索
       │                                  │
       │  11. tools/call 响应            │
       │<─────────────────────────────────┤
       │                                  │
       │  12. 解析结果                     │
       │                                  │
       │  [可以继续调用其他工具]           │
       │                                  │
       │  13. shutdown                    │
       ├─────────────────────────────────>│
       │                                  │
       │                                  │  14. 进程退出
       │                                  │
```

## 三、关键技术点

### 3.1 为什么使用 STDIO？

**优势**：
- ✅ **简单**: 不需要网络配置、端口管理
- ✅ **安全**: 进程间通信，不暴露网络端口
- ✅ **跨平台**: 所有操作系统都支持
- ✅ **性能**: 本地进程通信，延迟低

**限制**：
- ❌ **单对单**: 一个进程只能连接一个服务器
- ❌ **无网络**: 不能跨机器通信
- ❌ **进程依赖**: 进程退出，连接断开

### 3.2 消息边界问题

**问题**：STDIO 是流式传输，如何区分消息边界？

**解决方案**：使用换行符分隔（Line-delimited JSON）

```
消息1\n消息2\n消息3\n
```

每行是一个完整的 JSON 对象。

### 3.3 并发请求处理

**问题**：多个请求同时发送，如何匹配响应？

**解决方案**：使用请求 ID

```java
// 发送请求时记录 ID
int requestId = nextId();
pendingRequests.put(requestId, future);

// 接收响应时根据 ID 匹配
JsonNode response = receive();
int id = response.get("id").asInt();
CompletableFuture future = pendingRequests.get(id);
future.complete(response);
```

### 3.4 超时处理

```java
try {
    JsonNode response = future.get(20, TimeUnit.SECONDS);
    // 处理响应
} catch (TimeoutException e) {
    // 超时处理
    pendingRequests.remove(id);
    throw new IOException("请求超时", e);
}
```

## 四、调试技巧

### 4.1 查看进程

```bash
# 查看所有 npx 相关进程
ps aux | grep npx

# 查看进程树
pstree -p <Java进程ID>
```

### 4.2 查看 STDIO 通信

```java
// 在代码中添加日志
log.debug("发送消息: {}", jsonMessage);
log.debug("接收消息: {}", responseJson);
```

### 4.3 手动测试 MCP 服务器

```bash
# 直接运行 MCP 服务器
npx -y 12306-mcp

# 手动发送 JSON-RPC 消息测试
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' | npx -y 12306-mcp
```

## 总结

MCP 进程交互的核心：

1. **npx**: 自动下载并执行 npm 包，启动 MCP 服务器进程
2. **STDIO**: 通过标准输入输出进行进程间通信
3. **JSON-RPC**: 使用 JSON-RPC 2.0 协议进行消息交换
4. **请求-响应**: 通过请求 ID 匹配请求和响应
5. **异步处理**: 使用 Future 和线程处理并发请求

整个过程实现了 Java 应用与 Node.js MCP 服务器的无缝集成！
