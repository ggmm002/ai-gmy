将 Dify 接入 Spring Boot 业务系统是一个非常典型的 **LLM 应用落地场景**。Dify 提供了非常完善的 API，使得通过后端（Spring Boot）进行中转和业务逻辑封装变得简单高效。

以下我为你梳理的从 **Dify 配置** 到 **Spring Boot 代码实现** 的完整接入流程。

-----

### 核心架构设计

在开始写代码之前，理解数据流向至关重要。建议采用 **后端代理（Backend-for-Frontend）** 模式，而不是让前端直接调用 Dify。

  * **前端 (Web/App)**: 发送用户提问给 Spring Boot。
  * **Spring Boot**:
    1.  鉴权（验证当前业务系统用户）。
    2.  封装参数（关联 Dify 的 `user` 字段，处理 `conversation_id`）。
    3.  调用 Dify API。
    4.  流式透传（SSE）或全量返回 Dify 的响应给前端。
  * **Dify**: 处理 RAG 检索、推理，返回答案。

-----

### 第一阶段：Dify 侧准备 (配置与发布)

在写 Java 代码前，你需要确保 Dify 应用已经就绪：

1.  **创建知识库 (Datasets)**:
      * 在 Dify 中上传你的企业文档（PDF, Word, Markdown 等）。
      * 设置分段（Chunking）和清洗规则，等待索引完成。
2.  **创建应用 (Create App)**:
      * 选择 **"聊天助手 (Chat App)"** 类型。
      * 在编排页面的“上下文”中关联刚才创建的知识库。
      * 调试 Prompt，确保回答效果符合预期。
3.  **获取 API 凭证**:
      * 点击应用右上角的 **"访问 API"**。
      * 点击 **"API 密钥"** 生成一个新的 Key。
      * 记录下 `API Base URL` (如果是私有部署，通常是 `http://your-dify-host/v1`；如果是云端，则是 `https://api.dify.ai/v1`)。

-----

### 第二阶段：Spring Boot 接入实现

Spring Boot 主要负责调用 Dify 的 `/chat-messages` 接口。为了获得更好的用户体验（打字机效果），强烈建议使用 **Server-Sent Events (SSE)** 进行流式输出。

#### 1\. 引入依赖

如果使用 Spring Boot 3.x 或 WebFlux，推荐使用 `WebClient`；如果是传统 Servlet 栈，也可以使用 `OkHttp` 或 `Apache HttpClient`。这里演示 **WebClient (Spring WebFlux)** 方案，因为它处理流式响应最原生。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

#### 2\. 封装 Dify 请求对象

创建 DTO (Data Transfer Object) 来映射 Dify 的请求格式。

```java
import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class DifyChatRequest {
    private Map<String, Object> inputs; // 提示词变量
    private String query;               // 用户提问
    private String response_mode;       // "streaming" 或 "blocking"
    private String conversation_id;     // 会话ID，由Dify生成，第一次传空
    private String user;                // 业务系统的用户ID (必填)
    private Map<String, Object> files;  // 如果涉及图片上传
}
```

#### 3\. 编写 Service 层 (核心逻辑)

这是对接的核心。我们需要建立一个 Service 来通过 WebClient 发送请求并处理流。

```java
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Service
public class DifyService {

    private final WebClient webClient;
    // 建议配置在 application.yml 中
    private final String difyApiKey = "YOUR_DIFY_API_KEY"; 
    private final String difyBaseUrl = "https://api.dify.ai/v1"; 

    public DifyService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(difyBaseUrl)
                .defaultHeader("Authorization", "Bearer " + difyApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * 发送流式对话请求
     * @param query 用户问题
     * @param conversationId 会话ID (如果是新会话则为 null)
     * @param userId 业务系统当前用户ID
     * @return Flux<String> Dify返回的SSE流字符串
     */
    public Flux<String> chatStream(String query, String conversationId, String userId) {
        DifyChatRequest request = DifyChatRequest.builder()
                .inputs(Map.of()) // 如果你的Prompt没有变量，传空Map
                .query(query)
                .response_mode("streaming") // 开启流式
                .conversation_id(conversationId)
                .user(userId) // 关键：用于Dify区分不同用户的上下文
                .build();

        return webClient.post()
                .uri("/chat-messages")
                .bodyValue(request)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class); // 直接透传 JSON 字符串给前端解析
    }
}
```

#### 4\. 编写 Controller 层

对外暴露接口，支持 SSE。

```java
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeBotController {

    private final DifyService difyService;

    public KnowledgeBotController(DifyService difyService) {
        this.difyService = difyService;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody UserQueryRequest userRequest) {
        // 1. 获取当前登录用户 ID (假设从 SecurityContext 获取)
        String userId = "current_user_id_123"; 
        
        // 2. 调用 Dify
        return difyService.chatStream(
            userRequest.getQuery(), 
            userRequest.getConversationId(), 
            userId
        );
    }
}
```

-----

### 第三阶段：前端对接 (简单说明)

Spring Boot 接口现在的输出是 `text/event-stream` 格式。前端（Vue/React）需要使用 `EventSource` 或 `fetch` + `ReadableStream` 来读取。

Dify 返回的数据格式如下，前端需要解析 `answer` 字段：

```json
data: {"event": "message", "answer": "你好", "conversation_id": "..."}
data: {"event": "message", "answer": "，", "conversation_id": "..."}
data: {"event": "message", "answer": "我是", "conversation_id": "..."}
```

**注意：** 前端需要处理 `conversation_id`。第一次请求后，Dify 会返回一个 ID，前端需保存该 ID，在下一轮对话中传给后端，以保持上下文记忆。

-----

### 第四阶段：进阶功能 (知识库同步)

除了问答，企业业务系统通常需要**自动更新知识库**（例如：CMS系统中发布了一篇新文章，Dify 知识库也要自动更新）。

1.  **Dify 数据集 API**:
      * 使用 `/datasets/{dataset_id}/document/create_by_text` 接口。
2.  **Spring Boot 监听**:
      * 在你的业务系统中，使用 Spring Event 或 AOP 监听业务数据的“新增/更新”动作。
      * 触发 Dify Service 将清洗后的文本推送到 Dify 数据集。

-----

### 关键注意事项

| 关注点 | 建议 |
| :--- | :--- |
| **会话隔离** | 务必正确传递 `user` 字段。如果你传死值，所有用户的对话历史会混在一起。建议使用业务系统的 User ID (Hash处理后)。 |
| **安全性** | **绝对不要**把 Dify API Key 暴露在前端代码中。必须通过 Spring Boot 中转。 |
| **超时设置** | LLM 生成速度较慢，Spring Boot 的 HTTP Client 默认超时可能只有 30s，建议调整为 3-5 分钟。 |
| **引用来源** | Dify 的 API 返回中包含 `metadata` (引用片段)。建议解析并展示给用户：“答案参考了《员工手册》第3页”。 |
| **图片/文件** | 如果业务需要用户上传图片给 AI 分析，需要先调用 Dify 的 `/files/upload` 接口换取 file\_id，再调用 chat 接口。 |

-----

### 总结接入步骤清单

1.  **Dify**: 搭建知识库 -\> 调试 Prompt -\> 发布应用 -\> 拿 Key。
2.  **Spring Boot**: 引入 WebFlux -\> 封装 Request DTO -\> 写 WebClient 调用逻辑 (SSE) -\> 写 Controller。
3.  **前端**: 解析 SSE 流 -\> 渲染 Markdown -\> 维护 conversation\_id。
4.  **测试**: 验证多轮对话记忆是否生效，验证并发下响应速度。

**下一步建议：**
如果你现在需要具体的 `DifyChatRequest` 完整字段定义（包括如何处理文件上传），或者需要前端解析 SSE 的代码示例，请告诉我。