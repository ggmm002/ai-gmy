# npx vs node 命令区别说明

## 为什么使用不同的命令？

### 当前配置对比

```json
{
  "mcpServers": {
    // 使用 npx - 已发布的 npm 包
    "12306-mcp": {
      "command": "npx",
      "args": ["-y", "12306-mcp"]
    },
    
    // 使用 node - 本地开发的服务
    "mcp-weather-server": {
      "command": "node",
      "args": ["mcp-weather-server/index.js"]
    }
  }
}
```

## npx 命令

### 用途
`npx` 用于**执行 npm 包中的可执行文件**，特别是：

1. **从 npm 仓库下载并执行**
   - 如果包不在本地，自动从 npm 下载
   - 执行完后可以清理（可选）

2. **执行本地已安装的包**
   - 从 `node_modules/.bin` 中查找
   - 从全局包中查找

3. **临时执行，无需全局安装**

### 使用场景

**适用于**：
- ✅ 已发布到 npm 的包（如 `12306-mcp`）
- ✅ 需要自动下载和管理的包
- ✅ 不想全局安装的包

**示例**：
```bash
# npx 会自动下载并执行 12306-mcp 包
npx -y 12306-mcp

# 等价于：
# 1. npm install 12306-mcp (临时)
# 2. node node_modules/12306-mcp/index.js
# 3. npm uninstall 12306-mcp (可选)
```

### npx 执行流程

```
npx -y 12306-mcp
  │
  ├─> 检查本地是否有 12306-mcp
  │     │
  │     ├─> 有 → 直接执行
  │     │
  │     └─> 无 → 从 npm 下载到临时目录
  │               └─> 执行包的主程序
  │
  └─> 执行完成后（可选清理）
```

## node 命令

### 用途
`node` 用于**直接执行 JavaScript 文件**：

1. **执行本地文件**
   - 直接运行指定的 `.js` 文件
   - 不需要包管理器

2. **需要明确指定文件路径**
   - 必须提供完整的文件路径

### 使用场景

**适用于**：
- ✅ 本地开发的脚本/服务
- ✅ 未发布到 npm 的代码
- ✅ 需要精确控制执行路径的场景

**示例**：
```bash
# 直接执行本地文件
node mcp-weather-server/index.js

# 需要确保：
# 1. 文件路径正确
# 2. 依赖已安装（npm install）
# 3. Node.js 环境可用
```

### node 执行流程

```
node mcp-weather-server/index.js
  │
  ├─> 检查文件是否存在
  │     │
  │     └─> 不存在 → 报错
  │
  ├─> 解析文件（ES Module / CommonJS）
  │
  ├─> 加载依赖模块
  │     └─> 从 node_modules 查找
  │
  └─> 执行代码
```

## 两种方式对比

| 特性 | npx | node |
|------|-----|------|
| **适用对象** | npm 包 | 本地文件 |
| **是否需要发布** | ✅ 需要发布到 npm | ❌ 不需要 |
| **路径要求** | 包名即可 | 需要文件路径 |
| **依赖管理** | 自动处理 | 需要手动安装 |
| **版本控制** | 自动使用最新版 | 使用本地版本 |
| **使用场景** | 已发布的工具 | 本地开发的服务 |

## 为什么这次使用 node？

### 原因分析

1. **mcp-weather-server 是本地开发的服务**
   - 还没有发布到 npm
   - 代码在项目目录中
   - 需要直接执行本地文件

2. **12306-mcp 是已发布的 npm 包**
   - 已经发布到 npm 仓库
   - 可以通过包名直接访问
   - 使用 npx 更方便

### 如果使用 npx（需要先发布）

如果你想用 `npx` 方式，需要：

1. **发布到 npm**：
   ```bash
   cd mcp-weather-server
   npm publish
   ```

2. **然后配置**：
   ```json
   {
     "mcp-weather-server": {
       "command": "npx",
       "args": ["-y", "mcp-weather-server"]
     }
   }
   ```

3. **优点**：
   - 不需要指定文件路径
   - 自动管理版本
   - 可以在任何地方使用

4. **缺点**：
   - 需要发布和维护 npm 包
   - 修改代码需要重新发布
   - 不适合快速开发和调试

## 推荐使用方式

### 开发阶段（当前）

**使用 `node`**：
- ✅ 直接执行本地文件
- ✅ 修改代码立即生效
- ✅ 不需要发布流程
- ✅ 适合快速迭代

```json
{
  "command": "node",
  "args": ["mcp-weather-server/index.js"]
}
```

### 生产阶段（可选）

**使用 `npx`**（如果发布到 npm）：
- ✅ 统一管理
- ✅ 版本控制
- ✅ 易于部署

```json
{
  "command": "npx",
  "args": ["-y", "mcp-weather-server"]
}
```

## 混合使用场景

在实际项目中，可以同时使用两种方式：

```json
{
  "mcpServers": {
    // 使用 npx - 第三方已发布的包
    "12306-mcp": {
      "command": "npx",
      "args": ["-y", "12306-mcp"]
    },
    
    // 使用 node - 自己开发的本地服务
    "mcp-weather-server": {
      "command": "node",
      "args": ["mcp-weather-server/index.js"]
    },
    
    // 也可以使用 npx - 如果发布了自己的包
    "my-published-mcp": {
      "command": "npx",
      "args": ["-y", "@mycompany/mcp-service"]
    }
  }
}
```

## 总结

**为什么使用 node**：
- `mcp-weather-server` 是本地开发的服务
- 还没有发布到 npm
- 需要直接执行本地文件
- 适合快速开发和调试

**为什么 12306-mcp 使用 npx**：
- 已经发布到 npm 仓库
- 可以通过包名直接访问
- 自动管理下载和版本

**选择建议**：
- 🏠 **本地开发** → 使用 `node`（当前方式）
- 📦 **已发布的包** → 使用 `npx`
- 🚀 **生产环境** → 考虑发布后使用 `npx`，或继续使用 `node` + 绝对路径
