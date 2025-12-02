# MCP 天气查询服务

这是一个简单的 MCP (Model Context Protocol) 服务器，提供城市天气查询功能。

## 功能

- 查询指定城市的天气情况
- 固定返回：`XX市是晴天`

## 安装依赖

```bash
cd mcp-weather-server
npm install
```

## 本地开发测试

```bash
# 直接运行
node index.js

# 或者使用 npm
npm start
```

## 配置到 Spring AI

### 方式一：使用本地路径（推荐用于开发）

在 `src/main/resources/mcp-servers.json` 中配置：

```json
{
  "mcpServers": {
    "mcp-weather-server": {
      "command": "node",
      "args": [
        "mcp-weather-server/index.js"
      ],
      "env": {}
    }
  }
}
```

**注意**：需要确保 `node` 命令在系统 PATH 中可用。

### 方式二：发布为 npm 包（推荐用于生产）

1. 发布到 npm（可选，如果只是本地使用可以跳过）

```bash
npm publish
```

2. 在 `mcp-servers.json` 中使用 npx：

```json
{
  "mcpServers": {
    "mcp-weather-server": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-weather-server"
      ]
    }
  }
}
```

### 方式三：使用绝对路径

```json
{
  "mcpServers": {
    "mcp-weather-server": {
      "command": "node",
      "args": [
        "/absolute/path/to/mcp-weather-server/index.js"
      ]
    }
  }
}
```

## 工具定义

### get_weather

查询指定城市的天气情况。

**参数**：
- `city` (string, 必需): 城市名称，例如：北京、上海、广州

**返回**：
- 文本格式：`{city}市是晴天`

**示例**：
```json
{
  "name": "get_weather",
  "arguments": {
    "city": "北京"
  }
}
```

**响应**：
```
北京市是晴天
```

## 在 Java 应用中使用

1. 确保 `application.yml` 中 MCP 客户端已启用：

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
```

2. 重启应用，MCP 客户端会自动加载天气服务

3. 在 `MyAgentConfiguration` 中，`mcpSearchAgent` 会自动包含天气工具

4. 测试使用：

```bash
curl "http://localhost:7080/mcp/chat?question=查询北京的天气&userId=1"
```

## 调试

### 检查服务是否启动

查看应用日志，应该能看到：
```
已加载 X 个 MCP 工具
MCP 工具: get_weather
```

### 手动测试 MCP 服务

```bash
# 直接运行服务
cd mcp-weather-server
node index.js

# 手动发送 JSON-RPC 消息测试（需要按 MCP 协议格式）
```

### 查看错误日志

如果服务启动失败，检查：
1. Node.js 是否已安装：`node -v`
2. 依赖是否已安装：`cd mcp-weather-server && npm install`
3. 文件路径是否正确
4. 查看应用日志中的错误信息

## 扩展功能

如果需要扩展功能，可以修改 `index.js`：

1. **添加更多工具**：在 `ListToolsRequestSchema` 处理器中添加更多工具定义
2. **修改返回逻辑**：在 `CallToolRequestSchema` 处理器中修改天气查询逻辑
3. **添加参数验证**：增强输入参数的验证逻辑
4. **集成真实 API**：调用真实的天气 API 获取数据

## 文件结构

```
mcp-weather-server/
├── package.json          # npm 包配置
├── index.js              # MCP 服务器主程序
└── README.md             # 使用说明
```
