# MCP 工具集成代码示例（简化版）

## 模拟 Spring AI MCP 自动配置的核心逻辑

以下代码展示了 MCP 工具集成的核心实现逻辑（简化版，帮助理解原理）：

### 1. 自动配置类（Spring AI 内部实现）

```java
@Configuration
@ConditionalOnProperty(
    prefix = "spring.ai.mcp.client", 
    name = "enabled", 
    havingValue = "true"
)
public class McpClientAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public SyncMcpToolCallbackProvider syncMcpToolCallbackProvider(
            McpClientProperties properties,
            ResourceLoader resourceLoader) {
        
        // 步骤1: 读取 mcp-servers.json 配置
        McpServersConfig serversConfig = loadServersConfig(
            properties.getServersConfiguration(), 
            resourceLoader
        );
        
        // 步骤2: 创建 MCP 客户端
        McpClient mcpClient = new McpClient(serversConfig);
        
        // 步骤3: 初始化 MCP 客户端（启动进程、握手、获取工具）
        mcpClient.initialize();
        
        // 步骤4: 创建工具回调提供者
        return new SyncMcpToolCallbackProvider(mcpClient);
    }
    
    private McpServersConfig loadServersConfig(
            String configPath, 
            ResourceLoader resourceLoader) {
        try {
            Resource resource = resourceLoader.getResource(configPath);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(
                resource.getInputStream(), 
                McpServersConfig.class
            );
        } catch (Exception e) {
            throw new RuntimeException("加载 MCP 服务器配置失败", e);
        }
    }
}
```

### 2. MCP 客户端初始化（简化版）

```java
public class McpClient {
    private Map<String, Process> serverProcesses = new HashMap<>();
    private Map<String, List<Tool>> serverTools = new HashMap<>();
    
    public void initialize() {
        // 遍历配置中的每个 MCP 服务器
        for (Map.Entry<String, McpServerConfig> entry : 
             serversConfig.getMcpServers().entrySet()) {
            
            String serverName = entry.getKey();
            McpServerConfig config = entry.getValue();
            
            // 启动服务器进程
            Process process = startServerProcess(config);
            serverProcesses.put(serverName, process);
            
            // 建立 STDIO 通信
            StdioTransport transport = new StdioTransport(
                process.getInputStream(),
                process.getOutputStream()
            );
            
            // MCP 协议握手
            initializeMcpProtocol(transport, serverName);
        }
    }
    
    private Process startServerProcess(McpServerConfig config) {
        ProcessBuilder pb = new ProcessBuilder();
        
        // 构建命令: npx -y 12306-mcp
        List<String> command = new ArrayList<>();
        command.add(config.getCommand()); // "npx"
        command.addAll(config.getArgs());  // ["-y", "12306-mcp"]
        
        pb.command(command);
        pb.redirectErrorStream(true);
        
        return pb.start();
    }
    
    private void initializeMcpProtocol(
            StdioTransport transport, 
            String serverName) {
        
        // 1. 发送 initialize 请求
        InitializeRequest request = InitializeRequest.builder()
            .protocolVersion("2024-11-05")
            .capabilities(new ClientCapabilities())
            .clientInfo(new ClientInfo("ai-gmy", "1.0.0"))
            .build();
        
        transport.send(request);
        
        // 2. 接收 initialize 响应
        InitializeResponse response = transport.receive();
        
        // 3. 获取服务器提供的工具列表
        List<Tool> tools = response.getServerInfo().getTools();
        serverTools.put(serverName, tools);
        
        // 4. 发送 initialized 通知
        transport.send(new InitializedNotification());
        
        log.info("MCP 服务器 {} 初始化完成，提供 {} 个工具", 
                 serverName, tools.size());
    }
    
    public List<Tool> getTools() {
        // 合并所有服务器的工具
        return serverTools.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }
    
    public String callTool(String toolName, String arguments) {
        // 调用 MCP 工具
        ToolCallRequest request = ToolCallRequest.builder()
            .name(toolName)
            .arguments(arguments)
            .build();
        
        ToolCallResponse response = transport.sendAndReceive(request);
        return response.getContent();
    }
}
```

### 3. 工具回调提供者（简化版）

```java
public class SyncMcpToolCallbackProvider {
    private final McpClient mcpClient;
    
    public SyncMcpToolCallbackProvider(McpClient mcpClient) {
        this.mcpClient = mcpClient;
    }
    
    public ToolCallback[] getToolCallbacks() {
        // 获取所有 MCP 工具
        List<Tool> mcpTools = mcpClient.getTools();
        
        // 转换为 Spring AI 的 ToolCallback
        return mcpTools.stream()
            .map(this::convertToToolCallback)
            .toArray(ToolCallback[]::new);
    }
    
    private ToolCallback convertToToolCallback(Tool mcpTool) {
        return new ToolCallback() {
            @Override
            public String call(String arguments) {
                // 调用 MCP 服务器的工具
                return mcpClient.callTool(mcpTool.getName(), arguments);
            }
            
            @Override
            public ToolDefinition getToolDefinition() {
                // 转换为 Spring AI 的工具定义格式
                return ToolDefinition.builder()
                    .name(mcpTool.getName())
                    .description(mcpTool.getDescription())
                    .parameters(convertSchema(mcpTool.getInputSchema()))
                    .build();
            }
        };
    }
    
    private Map<String, Object> convertSchema(JsonSchema schema) {
        // 将 MCP 的 JSON Schema 转换为 Spring AI 的参数定义
        // 这里简化处理
        return Map.of(
            "type", schema.getType(),
            "properties", schema.getProperties()
        );
    }
}
```

### 4. 你的配置类使用方式

```java
@Configuration
public class MyAgentConfiguration {
    
    // Spring 自动注入（由自动配置类创建）
    @Autowired(required = false)
    private SyncMcpToolCallbackProvider mcpToolCallbackProvider;
    
    @Bean("mcpSearchAgent")
    public ReactAgent mcpSearchAgent() {
        // 如果 MCP 已启用，获取工具
        ToolCallback[] mcpTools = null;
        if (mcpToolCallbackProvider != null) {
            mcpTools = mcpToolCallbackProvider.getToolCallbacks();
            log.info("已加载 {} 个 MCP 工具", mcpTools.length);
        }
        
        // 创建 ReactAgent，传入 MCP 工具
        ReactAgent.Builder builder = ReactAgent.builder()
            .name("mcpSearchAgent")
            .model(chatModel)
            .systemPrompt(SYSTEM_MCP_SEARCH_PROMPT);
        
        if (mcpTools != null && mcpTools.length > 0) {
            builder.tools(mcpTools);  // 关键：这里传入 MCP 工具
        }
        
        return builder.build();
    }
}
```

## 关键点总结

### 1. 配置驱动的自动装配

```
application.yml (enabled: true)
    ↓
McpClientAutoConfiguration (自动配置类)
    ↓
创建 SyncMcpToolCallbackProvider Bean
```

### 2. 配置文件解析

```
mcp-servers.json
    ↓
ResourceLoader 加载文件
    ↓
ObjectMapper 解析 JSON
    ↓
McpServersConfig 对象
```

### 3. 进程启动和通信

```
ProcessBuilder.start()
    ↓
启动 npx -y 12306-mcp 进程
    ↓
建立 STDIO 通信通道
    ↓
MCP 协议握手
    ↓
获取工具列表
```

### 4. 工具转换和注入

```
MCP Tool 列表
    ↓
转换为 ToolCallback[]
    ↓
注入到 SyncMcpToolCallbackProvider
    ↓
通过 @Autowired 注入到你的配置类
    ↓
传递给 ReactAgent
```

## 为什么这样设计？

### 优势

1. **零代码集成**: 只需配置文件，无需编写连接代码
2. **进程隔离**: MCP 服务器独立进程，崩溃不影响主应用
3. **标准协议**: 使用 MCP 标准，任何符合协议的服务器都可集成
4. **动态加载**: 修改配置文件即可添加/移除服务器
5. **统一接口**: 所有工具都转换为 ToolCallback，统一使用方式

### 设计模式应用

- **自动配置模式**: Spring Boot 根据条件自动创建 Bean
- **提供者模式**: SyncMcpToolCallbackProvider 提供工具
- **适配器模式**: MCP Tool → ToolCallback 适配
- **工厂模式**: 自动配置类作为 Bean 工厂

## 调试建议

如果 MCP 工具没有加载，按以下顺序检查：

1. **检查配置**
   ```yaml
   spring.ai.mcp.client.enabled: true  # 必须为 true
   ```

2. **检查依赖**
   ```xml
   <dependency>
       <groupId>org.springframework.ai</groupId>
       <artifactId>spring-ai-starter-mcp-client</artifactId>
   </dependency>
   ```

3. **检查 JSON 格式**
   ```bash
   # 验证 JSON 格式
   cat src/main/resources/mcp-servers.json | jq .
   ```

4. **检查进程启动**
   ```bash
   # 查看是否有 npx 进程
   ps aux | grep npx
   ```

5. **查看日志**
   ```yaml
   logging:
     level:
       org.springframework.ai.mcp: DEBUG
   ```

## 总结

MCP 工具集成的核心是 **Spring Boot 的自动配置机制**：

1. 读取配置文件 → 2. 启动 MCP 服务器进程 → 3. 协议握手获取工具 → 4. 转换为 ToolCallback → 5. 注入到 Spring 容器 → 6. 提供给 ReactAgent

整个过程由 Spring AI MCP Starter 自动完成，你只需要：
- ✅ 配置 `application.yml`
- ✅ 配置 `mcp-servers.json`
- ✅ 注入 `SyncMcpToolCallbackProvider`
- ✅ 使用工具创建 ReactAgent

这就是为什么"配置就能用"的原因！
