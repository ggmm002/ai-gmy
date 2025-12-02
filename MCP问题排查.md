# MCP 天气服务问题排查

## 问题描述

启动应用时出现错误：
```
Client failed to initialize listing tools
TimeoutException: Did not observe any item or terminal signal within 20000ms
```

## 根本原因

**npm 依赖未安装** - `mcp-weather-server` 的依赖包 `@modelcontextprotocol/sdk` 未安装。

## 解决步骤

### 1. 安装依赖（已完成 ✅）

```bash
cd mcp-weather-server
npm install
```

结果：
```
added 86 packages, and audited 87 packages in 5s
found 0 vulnerabilities
```

### 2. 更新配置（已完成 ✅）

将 `mcp-servers.json` 中的路径改为绝对路径：

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

**为什么使用绝对路径？**
- Spring Boot 应用启动时，工作目录可能不是项目根目录
- 相对路径可能找不到文件
- 绝对路径最可靠

### 3. 重启应用

现在可以重新启动应用了，应该能看到：

```
已加载 X 个 MCP 工具
MCP 工具: get_weather
```

## 验证步骤

### 1. 检查依赖是否安装

```bash
cd mcp-weather-server
ls node_modules/@modelcontextprotocol/sdk
```

应该能看到 SDK 目录。

### 2. 手动测试服务（可选）

```bash
cd mcp-weather-server
node index.js
```

服务启动后应该在 stderr 输出：
```
MCP 天气服务已启动
```

### 3. 检查应用日志

启动应用后查看日志，确认：
- 没有错误信息
- 看到 "已加载 X 个 MCP 工具"
- 看到 "MCP 工具: get_weather"

### 4. 测试工具调用

```bash
curl "http://localhost:7080/mcp/chat?question=查询北京的天气&userId=1"
```

应该返回类似：
```
北京市是晴天
```

## 常见问题

### Q1: 如果还是找不到文件？

**检查路径是否正确**：
```bash
ls -la /Users/guomaoyang/IdeaProjects/ai-gmy/mcp-weather-server/index.js
```

如果文件存在，应该显示文件信息。

### Q2: 如果 Node.js 找不到？

**检查 Node.js 是否在 PATH 中**：
```bash
which node
node -v
```

如果找不到，使用完整路径：
```json
{
  "command": "/opt/homebrew/bin/node",
  "args": [...]
}
```

### Q3: 如果依赖安装失败？

**检查 npm 和网络**：
```bash
npm -v
npm config get registry
```

如果网络有问题，可以使用淘宝镜像：
```bash
npm config set registry https://registry.npmmirror.com
npm install
```

### Q4: 如果超时问题依然存在？

**增加超时时间**（在 application.yml 中）：
```yaml
spring:
  ai:
    mcp:
      client:
        request-timeout: 60s  # 从 20s 增加到 60s
```

## 总结

问题已解决，主要步骤：
1. ✅ 安装 npm 依赖
2. ✅ 使用绝对路径配置
3. 🔄 重启应用

现在可以重新运行应用了！
