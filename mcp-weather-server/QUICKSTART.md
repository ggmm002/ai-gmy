# MCP 天气服务快速开始

## 快速安装

### 1. 安装依赖

```bash
cd mcp-weather-server
npm install
```

或者使用安装脚本：

```bash
./install.sh
```

### 2. 配置 mcp-servers.json

已自动配置，路径为：`src/main/resources/mcp-servers.json`

当前配置（相对路径）：
```json
{
  "mcpServers": {
    "mcp-weather-server": {
      "command": "node",
      "args": [
        "mcp-weather-server/index.js"
      ]
    }
  }
}
```

**注意**：如果相对路径不工作，可以使用绝对路径：

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

### 3. 启动 Spring Boot 应用

```bash
mvn spring-boot:run
```

### 4. 验证工具是否加载

查看应用日志，应该能看到：

```
已加载 X 个 MCP 工具
MCP 工具: get_weather
```

### 5. 测试使用

```bash
# 使用 curl 测试
curl "http://localhost:7080/mcp/chat?question=查询北京的天气&userId=1"

# 或者在前端界面测试
# 访问 http://localhost:7080/index.html
# 选择 MCP 搜索智能体，输入：查询上海的天气
```

## 常见问题

### Q1: 工具没有加载？

**检查清单**：
1. ✅ Node.js 已安装：`node -v`
2. ✅ 依赖已安装：`cd mcp-weather-server && npm install`
3. ✅ 文件路径正确
4. ✅ `application.yml` 中 `spring.ai.mcp.client.enabled: true`
5. ✅ 查看应用日志中的错误信息

### Q2: 路径找不到？

**解决方案**：
1. 使用绝对路径（推荐）
2. 或者创建一个启动脚本，在 `mcp-servers.json` 中使用脚本路径

### Q3: 如何调试？

**方法**：
1. 直接运行服务测试：
   ```bash
   cd mcp-weather-server
   node index.js
   ```
2. 查看应用日志：`tail -f application.log`
3. 启用 DEBUG 日志：
   ```yaml
   logging:
     level:
       org.springframework.ai.mcp: DEBUG
   ```

## 工具使用示例

### 在 Agent 中使用

Agent 会自动识别并调用天气工具：

```
用户: 查询北京的天气
Agent: 调用 get_weather 工具，参数: {"city": "北京"}
工具返回: 北京市是晴天
Agent: 根据工具返回的结果，北京市是晴天
```

### 工具参数

- `city` (string, 必需): 城市名称
  - 示例: "北京"、"上海"、"广州"

### 工具返回

- 格式: `{city}市是晴天`
- 示例: "北京市是晴天"
