# AI-GMY 项目 Wiki

## 项目概述

AI-GMY 是一个基于 Spring Boot 和 Spring AI Alibaba Agent Framework 构建的智能汽车销售助手系统。该项目集成了阿里云 DashScope 的 AI 模型，为汽车门店提供智能化的客户服务能力，包括文本对话、图片分析、订单处理、RAG（检索增强生成）、多智能体协作等功能。

**核心特性**:
- 🤖 **5个智能体**: 基础对话、视觉理解、人机交互、RAG检索、多智能体协作
- 🛠️ **5个工具**: 账号查询、品牌查询、下单、车型查询、向量搜索
- 🔒 **内容安全**: 敏感词过滤、性能监控
- 📚 **知识库**: 基于 Milvus 的向量检索和 RAG 问答
- 👥 **多智能体**: 支持智能体间的协作和编排

## 技术栈

- **框架**: Spring Boot 3.5.7
- **Java 版本**: 17
- **AI 框架**: Spring AI Alibaba Agent Framework 1.1.0.0-M5
- **AI 模型**: 阿里云 DashScope (通义千问系列)
  - `qwen-max`: 通用对话模型
  - `qwen3-max`: 增强对话模型
  - `qwen3-vl-plus`: 视觉理解模型
  - `qwen-vl-max-latest`: 视觉理解模型（最新版）
  - `text-embedding-v4`: 文本嵌入模型（用于向量化）
- **向量数据库**: Milvus
- **构建工具**: Maven
- **主要依赖**:
  - Spring Boot Starter Web
  - Spring AI Alibaba Agent Framework 1.1.0.0-M5
  - Spring AI Alibaba Starter DashScope 1.1.0.0-M5
  - Spring AI Milvus Store 1.1.0
  - Spring AI Autoconfigure Vector Store Milvus 1.1.0
  - Spring AI Advisors Vector Store 1.1.0
  - Lombok 1.18.30

## 项目结构

```
ai-gmy/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/aigmy/
│   │   │       ├── AiGmyApplication.java          # 主应用入口
│   │   │       ├── config/
│   │   │       │   ├── MyAgentConfiguration.java   # 智能体配置类
│   │   │       │   └── VectorDataInit.java         # 向量数据初始化（已注释）
│   │   │       ├── controller/
│   │   │       │   ├── FirstController.java       # 基础对话和图片分析接口
│   │   │       │   ├── HitlController.java        # 人机交互接口
│   │   │       │   ├── MultiController.java       # 多智能体协作接口
│   │   │       │   └── RAGController.java         # RAG 向量检索接口
│   │   │       ├── dto/
│   │   │       │   ├── ArticleRequest.java        # 文章请求DTO
│   │   │       │   ├── ArticleOutput.java        # 文章输出DTO
│   │   │       │   └── ReviewOutput.java         # 评审输出DTO
│   │   │       ├── interceptor/
│   │   │       │   ├── ContentInterceptor.java    # 内容拦截器（敏感词过滤）
│   │   │       │   └── ModelPerformanceInterceptor.java  # 性能监控拦截器
│   │   │       └── tool/
│   │   │           ├── AccountInfoTool.java        # 账号信息查询工具
│   │   │           ├── CarBrandTool.java           # 汽车品牌查询工具
│   │   │           ├── PlaceOrderTool.java         # 下单工具
│   │   │           ├── SaleCarsInfoTool.java       # 车型信息查询工具
│   │   │           └── VectorSearchTool.java       # 向量搜索工具
│   │   └── resources/
│   │       ├── application.yml                     # 应用配置文件（YAML格式）
│   │       ├── test.txt                            # 测试文档（用于RAG向量化）
│   │       ├── img/                                # 图片资源目录
│   │       └── static/                             # 静态资源目录
│   │           └── index.html                      # 前端页面
│   └── test/                                       # 测试代码
└── pom.xml                                         # Maven 配置文件
```

## 核心功能

### 1. 智能体（Agents）

项目包含五个核心智能体，每个智能体都有不同的用途和配置：

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

#### 1.4 ragAgent（检索增强生成智能体）

- **模型**: `qwen-max`
- **功能**: 基于向量知识库的智能问答
- **工具**: 
  - `vectorSearchTool`（向量搜索工具）
- **特性**: 
  - 集成了 `RetrievalAugmentationAdvisor`，自动从向量数据库中检索相关信息
  - 相似度阈值设置为 0.50
  - 温度参数设置为 0.3，确保回答更加准确
  - 最大token数设置为 2000
- **系统提示词**: 定义为基于知识库的智能助手，优先使用知识库中的信息回答问题

#### 1.5 multiAgent（多智能体协作智能体）

- **模型**: `qwen3-max`
- **功能**: 协调多个子智能体完成复杂任务
- **子智能体**:
  - `writerAgent`（写作智能体）: 根据结构化输入（topic、wordCount、style）创作文章，返回结构化输出（title、content、characterCount）
  - `reviewerAgent`（评审智能体）: 对文章进行评审，返回评审意见（comment、approved、suggestions）
- **特性**: 
  - 使用 `AgentTool.getFunctionToolCallback()` 将子智能体作为工具集成
  - 支持类型化的输入输出（使用 DTO 类）
  - 实现写作和评审的自动化流程

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

#### 2.5 VectorSearchTool（向量搜索工具）

- **功能**: 从向量数据库中检索与查询内容相关的文档
- **参数**: 用户查询的问题或关键词（String）
- **特性**: 
  - 默认返回 topK = 5 个最相似的文档
  - 自动处理空查询情况
  - 将多个文档内容合并为字符串返回（使用分隔符 `\n\n---\n\n`）
- **返回**: 相关文档内容（字符串格式），如果未找到则返回提示信息

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

#### 4.3 RAGController

##### GET `/rag/vectorAdd`
- **功能**: 将文档添加到向量数据库
- **参数**: 无
- **返回**: void
- **实现逻辑**:
  1. 从 `src/main/resources/test.txt` 加载文档
  2. 使用 `TokenTextSplitter` 将文档分割成块（chunk size: 64）
  3. 分批添加到 Milvus 向量存储（每批最多10个文档块）
- **日志**: 输出批次添加进度信息

##### GET `/rag/vectorSearch`
- **功能**: 在向量数据库中执行相似度搜索
- **参数**: 
  - `query` (String): 查询文本
- **返回**: 相似文档列表（字符串格式）
- **实现逻辑**:
  1. 使用查询文本在 Milvus 向量存储中执行相似度搜索
  2. 返回 topK 个最相似的文档（topK = query.length()）

##### GET `/rag/chat`
- **功能**: 使用 RAG Agent 进行智能问答
- **参数**: 
  - `question` (String): 用户问题
- **返回**: AI 的回答（基于向量知识库检索）
- **特性**: 
  - Agent 会自动从向量知识库中检索相关信息
  - 结合检索到的信息生成回答
  - 如果知识库中没有相关信息，会诚实告知用户

##### GET `/rag/chatWithContext`
- **功能**: 使用 RAG Agent 进行带会话上下文的问答
- **参数**: 
  - `question` (String): 用户问题
  - `userId` (Long): 用户ID（用于会话隔离）
- **返回**: AI 的回答（基于向量知识库检索）
- **特性**: 
  - 支持多轮对话，每次对话都会保留上下文
  - 使用 `threadId` 实现会话隔离
  - 每个用户通过 `userId` 维护独立的对话上下文

#### 4.4 MultiController

##### GET `/multi/chat`
- **功能**: 多智能体协作对话接口
- **参数**: 
  - `question` (String): 用户问题（应包含文章主题、字数、风格等信息）
  - `userId` (Long): 用户ID
- **返回**: 多智能体协作的结果（包含写作和评审结果）
- **特性**: 
  - 协调 `writerAgent` 和 `reviewerAgent` 完成写作和评审流程
  - 支持类型化的输入输出
  - 使用 `threadId` 实现会话隔离

## 配置说明

### application.yml

```yaml
server:
  port: 7080

spring:
  application:
    name: ai-gmy
  ai:
    dashscope:
      embedding:
        options:
          model: text-embedding-v4
          dimensions: 1536
      chat:
        options:
          model: qwen-max
      api-key: sk-883eca171d2d434eab7e4d503987156f
    vectorstore:
      milvus:
        client:
          host: ${MILVUS_HOST:localhost}
          port: ${MILVUS_PORT:19530}
          username: ${MILVUS_USERNAME:root}
          password: ${MILVUS_PASSWORD:milvus}
        databaseName: ${MILVUS_DATABASE_NAME:default}
        collectionName: ${MILVUS_COLLECTION_NAME:vector_store}
        embeddingDimension: 1536
        indexType: IVF_FLAT
        metricType: COSINE
```

**重要配置说明**:

1. **DashScope API Key**: 请将 `spring.ai.dashscope.api-key` 替换为您自己的阿里云 DashScope API Key
2. **Milvus 配置**: 
   - 支持通过环境变量配置 Milvus 连接信息
   - 默认连接到本地 Milvus 服务（localhost:19530）
   - 向量维度为 1536（与 text-embedding-v4 模型匹配）
   - 使用 IVF_FLAT 索引类型和 COSINE 相似度度量
3. **环境变量**:
   - `MILVUS_HOST`: Milvus 服务器地址（默认: localhost）
   - `MILVUS_PORT`: Milvus 服务器端口（默认: 19530）
   - `MILVUS_USERNAME`: Milvus 用户名（默认: root）
   - `MILVUS_PASSWORD`: Milvus 密码（默认: milvus）
   - `MILVUS_DATABASE_NAME`: 数据库名称（默认: default）
   - `MILVUS_COLLECTION_NAME`: 集合名称（默认: vector_store）

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

### 6. RAG 向量检索

#### 6.1 添加文档到向量数据库

```bash
curl "http://localhost:7080/rag/vectorAdd"
```

此接口会读取 `src/main/resources/test.txt` 文件，将其分割后添加到 Milvus 向量数据库。

#### 6.2 向量相似度搜索

```bash
curl "http://localhost:7080/rag/vectorSearch?query=SpringAIAlibaba如何创建项目"
```

此接口会根据查询文本在向量数据库中进行相似度搜索，返回相关文档。

#### 6.3 RAG 智能问答

```bash
curl "http://localhost:7080/rag/chat?question=SpringAIAlibaba如何创建项目"
```

此接口会使用 RAG Agent 自动从向量知识库中检索相关信息并生成回答。

#### 6.4 RAG 带上下文的问答

```bash
curl "http://localhost:7080/rag/chatWithContext?question=SpringAIAlibaba如何创建项目&userId=1"
```

此接口支持多轮对话，每次对话都会保留上下文。

### 7. 多智能体协作

```bash
curl "http://localhost:7080/multi/chat?question=请写一篇关于人工智能的1000字文章，风格要专业&userId=1"
```

此接口会协调写作智能体和评审智能体，完成文章的创作和评审流程。

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

### DTO 数据模型

#### ArticleRequest（文章请求）

```java
- topic: String - 文章主题
- wordCount: int - 文章字数
- style: String - 文章风格
```

#### ArticleOutput（文章输出）

```java
- title: String - 文章标题
- content: String - 文章内容
- characterCount: int - 字符数
```

#### ReviewOutput（评审输出）

```java
- comment: String - 评审意见
- approved: boolean - 是否通过
- suggestions: List<String> - 建议列表
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

### 6. RAG（检索增强生成）

- 使用 Milvus 向量数据库存储文档向量
- 支持文档的向量化和相似度搜索
- 使用 `TokenTextSplitter` 将长文档分割成小块
- 使用 DashScope 的 `text-embedding-v4` 模型进行文本向量化
- 支持批量添加文档到向量存储
- 集成 `RetrievalAugmentationAdvisor` 实现自动检索增强
- 支持通过 RAG Agent 进行智能问答

### 7. 多智能体协作

- 支持将多个智能体作为工具集成到主智能体中
- 实现复杂的多步骤任务流程（如写作+评审）
- 支持类型化的输入输出（使用 DTO 类）
- 通过 `AgentTool.getFunctionToolCallback()` 实现智能体间的调用

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

### 6. RAG 功能增强

- ✅ 已集成 RAG 功能到智能体中（ragAgent），实现基于知识库的回答
- 支持多种文档格式（PDF、Word、Markdown 等）
- 优化文档分块策略，提高检索精度
- 实现查询结果的相关性排序和过滤
- ✅ 已支持多轮对话中的上下文检索（通过 `/rag/chatWithContext` 接口）

### 7. 多智能体协作扩展

- ✅ 已实现基础的多智能体协作（multiAgent）
- 支持更多类型的子智能体（如翻译、摘要等）
- 实现智能体间的条件分支和循环逻辑
- 支持智能体的动态组合和编排

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

### Q: 如何使用 RAG 功能？

A: 
1. **准备 Milvus 服务**: 确保 Milvus 服务已启动并可通过配置的地址访问
2. **添加文档**: 调用 `/rag/vectorAdd` 接口将文档添加到向量数据库
3. **执行搜索**: 调用 `/rag/vectorSearch` 接口进行相似度搜索
4. **智能问答**: 调用 `/rag/chat` 或 `/rag/chatWithContext` 接口使用 RAG Agent 进行智能问答
5. **自定义文档**: 修改 `RAGController.vectorAdd()` 方法中的文件路径，或扩展支持更多文档格式

### Q: 如何使用多智能体协作功能？

A: 
1. 调用 `/multi/chat` 接口，传入包含任务描述的问题
2. multiAgent 会自动协调 writerAgent 和 reviewerAgent 完成写作和评审流程
3. 返回结果包含完整的文章内容和评审意见
4. 可以通过修改 `MyAgentConfiguration.multiAgent()` 方法添加更多子智能体

### Q: 如何配置 Milvus 连接？

A: 
1. **本地 Milvus**: 使用默认配置即可（localhost:19530）
2. **远程 Milvus**: 通过环境变量设置 `MILVUS_HOST` 和 `MILVUS_PORT`
3. **认证信息**: 通过 `MILVUS_USERNAME` 和 `MILVUS_PASSWORD` 环境变量配置

### Q: 如何调整向量检索的 topK 值？

A: 
- **RAGController.vectorSearch()**: 修改方法中的 `topK()` 参数。当前实现为 `query.length()`，可以根据实际需求调整。
- **VectorSearchTool**: 修改 `DEFAULT_TOP_K` 常量（默认值为 5）
- **ragAgent**: 修改 `RetrievalAugmentationAdvisor` 中的 `similarityThreshold` 参数（当前为 0.50）

## 更新日志

### 2025/12/1
- 新增 `multiAgent` 多智能体协作功能
- 新增 `MultiController` 控制器
- 新增 `VectorSearchTool` 向量搜索工具
- 新增 `ragAgent` 检索增强生成智能体
- 新增 RAG 相关的 API 接口（`/rag/chat`, `/rag/chatWithContext`）
- 新增 DTO 类（`ArticleRequest`, `ArticleOutput`, `ReviewOutput`）

### 2025/11/23
- 新增 `ContentInterceptor` 内容拦截器
- 新增 `ModelPerformanceInterceptor` 性能监控拦截器
- 新增 `HitlController` 人机交互控制器
- 新增 `hitlAgent` 人机交互智能体

### 2025/11/22
- 项目初始版本
- 实现基础对话功能
- 实现图片分析功能
- 实现 RAG 向量检索功能

## 版本信息

- **项目版本**: 0.0.1-SNAPSHOT
- **Spring Boot**: 3.5.7
- **Spring AI Alibaba**: 1.1.0.0-M5
- **Spring AI**: 1.1.0-M4
- **Java**: 17

## 作者

- guomaoyang (2025/11/22 - 2025/12/1)

## 许可证

待定

---

**注意**: 本文档会随着项目的发展持续更新，请定期查看最新版本。
