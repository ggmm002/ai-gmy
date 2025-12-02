# MCP 天气服务修复说明

## 问题分析

### 第一个问题：依赖未安装 ✅ 已解决
```bash
cd mcp-weather-server
npm install
```

### 第二个问题：导入错误 ✅ 已解决

**错误信息**：
```
SyntaxError: The requested module '@modelcontextprotocol/sdk/types.js' 
does not provide an export named 'Tool'
```

**原因**：
代码中导入了不存在的 `Tool` 类型

**修复**：
移除了 `Tool` 的导入：

```javascript
// 修复前
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  Tool,  // ❌ 这个导入不存在
} from "@modelcontextprotocol/sdk/types.js";

// 修复后
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
```

## 已完成的修复

1. ✅ 安装 npm 依赖
2. ✅ 修复代码导入错误
3. ✅ 使用绝对路径配置

## 当前配置

### mcp-servers.json
```json
{
  "mcpServers": {
    "mcp-weather-server": {
      "command": "node",
      "args": [
        "/Users/guomaoyang/IdeaProjects/ai-gmy/mcp-weather-server/index.js"
      ]
    }
  }
}
```

### index.js (已修复)
- 移除了不存在的 `Tool` 导入
- 保留了必要的 Schema 导入

## 现在可以重启应用

重新启动 Spring Boot 应用，应该能成功加载 MCP 工具。

### 预期日志输出

```
已加载 X 个 MCP 工具
MCP 工具: get_weather
```

### 测试命令

```bash
curl "http://localhost:7080/mcp/chat?question=查询北京的天气&userId=1"
```

预期返回：
```
北京市是晴天
```

## 如果还有问题

### 1. 增加超时时间（application.yml）

```yaml
spring:
  ai:
    mcp:
      client:
        request-timeout: 60s  # 从 20s 增加到 60s
```

### 2. 启用调试日志

```yaml
logging:
  level:
    org.springframework.ai.mcp: DEBUG
    io.modelcontextprotocol: DEBUG
```

### 3. 手动测试 MCP 协议

创建测试脚本 `test-mcp.js`:

```javascript
// 手动发送 MCP 初始化消息测试
const message = {
  jsonrpc: "2.0",
  id: 1,
  method: "initialize",
  params: {
    protocolVersion: "2024-11-05",
    capabilities: {},
    clientInfo: {
      name: "test-client",
      version: "1.0.0"
    }
  }
};

console.log(JSON.stringify(message));
```

运行：
```bash
node test-mcp.js | node mcp-weather-server/index.js
```

应该返回初始化响应和工具列表。
