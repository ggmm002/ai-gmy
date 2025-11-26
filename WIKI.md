# AI-GMY 项目 Wiki

## 项目概述

AI-GMY 是一个基于 Spring Boot 和 Spring AI Alibaba Agent Framework 构建的智能汽车销售助手系统。该项目集成了阿里云 DashScope 的 AI 模型，为汽车门店提供智能化的客户服务能力，包括文本对话、图片分析、订单处理等功能。

## 技术栈

- **框架**: Spring Boot 3.5.7
- **Java 版本**: 17
- **AI 框架**: Spring AI Alibaba Agent Framework 1.1.0.0-M5
- **AI 模型**: 阿里云 DashScope (通义千问系列)
  - `qwen-max`: 通用对话模型
  - `qwen3-max`: 增强对话模型
  - `qwen3-vl-plus`: 视觉理解模型
  - `qwen-vl-max-latest`: 视觉理解模型（最新版）
- **构建工具**: Maven
- **其他依赖**: Lombok

## 项目结构

```
ai-gmy/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/aigmy/
│   │   │       ├── AiGmyApplication.java          # 主应用入口
│   │   │       ├── config/
│   │   │       │   └── MyAgentConfiguration.java   # 智能体配置类
│   │   │       ├── controller/
│   │   │       │   ├── FirstController.java       # 基础对话和图片分析接口
│   │   │       │   └── HitlController.java        # 人机交互接口
│   │   │       ├── interceptor/
│   │   │       │   ├── ContentInterceptor.java    # 内容拦截器（敏感词过滤）
│   │   │       │   └── ModelPerformanceInterceptor.java  # 性能监控拦截器
│   │   │       └── tool/
│   │   │           ├── AccountInfoTool.java        # 账号信息查询工具
│   │   │           ├── CarBrandTool.java           # 汽车品牌查询工具
│   │   │           ├── PlaceOrderTool.java         # 下单工具
│   │   │           └── SaleCarsInfoTool.java       # 车型信息查询工具
│   │   └── resources/
│   │       ├── application.properties              # 应用配置文件
│   │       └── img/                                # 图片资源目录
│   └── test/                                       # 测试代码
└── pom.xml                                         # Maven 配置文件
```

## 核心功能

### 1. 智能体（Agents）

项目包含三个核心智能体，每个智能体都有不同的用途和配置：

#### 1.1 firstAgent（基础对话智能体）

- **模型**: `qwen-max`
- **功能**: 处理汽车门店相关的客户咨询
- **工具**: `carBrandTool`（汽车品牌查询）
- **拦截器**: 
  - `ContentInterceptor`（内容过滤）
  - `ModelPerformanceInterceptor`（性能监控）
- **系统提示词**: 定义为汽车门店销售经理，能够回答关于汽车品牌、型号、价格、颜色、配置、优惠政策等问题

#### 1.2 vlAgent（视觉理解智能体）

- **模型**: `qwen3-vl-plus`
- **功能**: 图片内容分析和理解
- **特性**: 支持多模态输入（`withMultiModel(true)`）
- **系统提示词**: 专业的视觉理解模型，实现图像理解、图像生成、图像编辑、图像分析等能力

#### 1.3 hitlAgent（人机交互智能体）

- **模型**: `qwen3-max`
- **功能**: 支持人工审批流程的智能体
- **工具**: 
  - `accountInfoTool`（账号信息查询）
  - `placeOrderTool`（下单工具，需要人工审批）
  - `saleCarsInfoTool`（车型信息查询）
- **特性**: 集成了 `HumanInTheLoopHook`，当调用 `placeOrderTool` 时会触发人工审批流程

### 2. 工具（Tools）

#### 2.1 AccountInfoTool（账号信息查询工具）

- **功能**: 查询当前用户的账号信息
- **参数**: 可选字符串参数
- **数据源**: 内置用户数据库（模拟数据）
- **返回信息**: 用户姓名、账号类型、余额、邮箱等

#### 2.2 CarBrandTool（汽车品牌查询工具）

- **功能**: 查询门店销售的汽车品牌
- **返回**: 奔驰、宝马、奥迪

#### 2.3 PlaceOrderTool（下单工具）

- **功能**: 处理客户下单操作
- **参数**: 客户信息和下单的汽车品牌车型信息
- **特性**: 需要人工审批（通过 HumanInTheLoopHook 实现）
- **返回**: 下单成功信息（包含用户ID）

#### 2.4 SaleCarsInfoTool（车型信息查询工具）

- **功能**: 查询门店销售的车型详细信息
- **参数**: 可选的车型名称
- **返回信息**: 车型名称、价格、颜色、库存等

### 3. 拦截器（Interceptors）

#### 3.1 ContentInterceptor（内容拦截器）

- **功能**: 过滤敏感词汇
- **实现**: 检查用户输入是否包含敏感词，如果包含则返回提示信息
- **当前敏感词列表**: "草泥马", "敏感词2", "敏感词3"

#### 3.2 ModelPerformanceInterceptor（性能监控拦截器）

- **功能**: 监控模型调用性能
- **记录信息**: 
  - 调用消息条数
  - 模型响应耗时（毫秒）

### 4. API 接口

#### 4.1 FirstController

##### GET `/chat`
- **功能**: 基础对话接口
- **参数**: 
  - `question` (String): 用户问题
- **返回**: 智能体的响应结果

##### GET `/chat2`
- **功能**: 支持用户会话的对话接口
- **参数**: 
  - `question` (String): 用户问题
  - `userId` (Long): 用户ID（用于会话隔离）
- **返回**: 智能体的响应结果
- **特性**: 使用 `threadId` 实现会话隔离，支持多轮对话

##### POST `/analyzeImage`
- **功能**: 分析图片内容
- **参数**: 
  - `imageUrl` (String): 图片URL
  - `message` (String, 可选): 自定义提示词，默认为"描述这张图片的内容。"
- **返回**: AI 对图片的描述
- **使用智能体**: `vlAgent`

##### GET `/test`
- **功能**: 测试图片分析功能（使用本地资源图片）
- **返回**: AI 对图片的描述
- **测试图片**: `resources/img/123.png`

##### GET `/image`
- **功能**: 图片分析接口（使用 ChatClient）
- **参数**: 
  - `prompt` (String, 可选): 提示词，默认为"这些是什么？"
- **返回**: AI 对图片的描述
- **模型**: `qwen-vl-max-latest`

#### 4.2 HitlController

##### GET `/hitl/chat`
- **功能**: 人机交互对话接口
- **参数**: 
  - `question` (String): 用户问题
  - `userId` (Long): 用户ID
- **返回**: 智能体的响应结果
- **特性**: 
  - 当调用需要审批的工具（如 `placeOrderTool`）时，会中断执行并返回审批信息
  - 支持人工审批后继续执行
  - 自动处理审批流程（当前实现为自动批准）

## 配置说明

### application.properties

```properties
spring.application.name=ai-gmy
server.port=7080
spring.ai.dashscope.chat.options.model=qwen-max
spring.ai.dashscope.api-key=sk-883eca171d2d434eab7e4d503987156f
```

**重要**: 请将 `spring.ai.dashscope.api-key` 替换为您自己的阿里云 DashScope API Key。

## 使用示例

### 1. 启动应用

```bash
mvn spring-boot:run
```

应用将在 `http://localhost:7080` 启动。

### 2. 基础对话

```bash
curl "http://localhost:7080/chat?question=你们店有哪些汽车品牌？"
```

### 3. 带用户会话的对话

```bash
curl "http://localhost:7080/chat2?question=你们店有哪些汽车品牌？&userId=1"
```

### 4. 图片分析

```bash
curl -X POST "http://localhost:7080/analyzeImage?imageUrl=https://example.com/image.jpg"
```

### 5. 人机交互下单

```bash
curl "http://localhost:7080/hitl/chat?question=我要下单购买奔驰C260L&userId=1"
```

## 数据模型

### 用户数据（AccountInfoTool）

```java
用户ID: 1
- 姓名: 爱丽丝
- 账号类型: Premium
- 余额: 5000
- 邮箱: alice@example.com

用户ID: 2
- 姓名: 鲍勃
- 账号类型: Standard
- 余额: 1200
- 邮箱: bob@example.com
```

### 车型数据（SaleCarsInfoTool）

```java
车型ID: 1 - 奔驰C260L
- 价格: 300000
- 颜色: 白色
- 库存: 3

车型ID: 2 - 宝马325Li
- 价格: 200000
- 颜色: 黑色
- 库存: 0

车型ID: 3 - 宝马330Li
- 价格: 250000
- 颜色: 红色
- 库存: 5

车型ID: 4 - 奥迪A4L
- 价格: 220000
- 颜色: 蓝色
- 库存: 6

车型ID: 5 - 奥迪A6L
- 价格: 280000
- 颜色: 绿色
- 库存: 1
```

## 核心特性

### 1. 会话管理

- 使用 `RunnableConfig` 和 `threadId` 实现会话隔离
- 每个用户通过 `userId` 维护独立的对话上下文
- 使用 `MemorySaver` 保存对话历史

### 2. 人工审批流程（HITL）

- 通过 `HumanInTheLoopHook` 实现工具调用的审批机制
- 当智能体调用需要审批的工具时，会中断执行并返回审批信息
- 支持审批后继续执行流程

### 3. 多模态支持

- 支持文本对话
- 支持图片分析（通过视觉理解模型）
- 支持图片URL和本地资源

### 4. 内容安全

- 通过 `ContentInterceptor` 实现敏感词过滤
- 可扩展添加更多敏感词

### 5. 性能监控

- 通过 `ModelPerformanceInterceptor` 监控模型调用性能
- 记录消息条数和响应时间

## 开发规范

根据项目规则，开发时需要注意：

1. **禁止使用反射操作对象属性**: 根据对象属性的注释或字段名推断并选中属性再进行对应的操作
2. **日志输出规范**: 在日志中打印对象时，需要使用 JSON 序列化方法（如 `JSON.toJSONString(object)`）进行转换，避免直接输出对象引用
3. **中文注释**: 项目使用中文进行注释和文档说明

## 扩展建议

### 1. 数据持久化

当前工具类使用内存数据，建议：
- 集成数据库（如 MySQL、PostgreSQL）
- 实现数据访问层（DAO/Repository）
- 支持数据的增删改查操作

### 2. 用户认证

- 集成 Spring Security
- 实现用户登录、注册功能
- 支持 JWT Token 认证

### 3. 审批流程优化

- 实现真实的审批界面
- 支持审批历史记录
- 支持审批通知（邮件、短信等）

### 4. 更多工具扩展

- 库存查询工具
- 价格计算工具
- 优惠活动查询工具
- 试驾预约工具

### 5. 监控和日志

- 集成日志框架（如 Logback、Log4j2）
- 添加应用监控（如 Prometheus、Micrometer）
- 实现日志聚合和分析

## 常见问题

### Q: 如何更换 AI 模型？

A: 在 `MyAgentConfiguration.java` 中修改 `DashScopeChatOptions.builder().withModel()` 的参数，可选的模型包括：
- `qwen-max`
- `qwen3-max`
- `qwen3-vl-plus`
- `qwen-vl-max-latest`

### Q: 如何添加新的工具？

A: 
1. 创建工具类，实现 `Function` 或 `Supplier` 接口
2. 在 `MyAgentConfiguration.init()` 方法中注册工具
3. 在智能体配置中使用 `.tools()` 方法添加工具

### Q: 如何修改敏感词列表？

A: 在 `ContentInterceptor.java` 中修改 `BLOCKED_WORDS` 列表。

### Q: 如何实现真实的审批流程？

A: 
1. 修改 `HitlController.chat()` 方法，将自动批准改为返回审批信息
2. 创建审批接口，接收审批结果
3. 使用审批结果继续执行智能体流程

## 版本信息

- **项目版本**: 0.0.1-SNAPSHOT
- **Spring Boot**: 3.5.7
- **Spring AI Alibaba**: 1.1.0.0-M5
- **Java**: 17

## 作者

- guomaoyang (2025/11/22)

## 许可证

待定

---

**注意**: 本文档会随着项目的发展持续更新，请定期查看最新版本。
