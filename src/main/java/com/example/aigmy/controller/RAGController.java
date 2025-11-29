package com.example.aigmy.controller;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/rag")
@Slf4j
public class RAGController {


    @Autowired
    private MilvusVectorStore vectorStore;

    @Autowired
    @Qualifier("ragAgent")
    private ReactAgent ragAgent;

    @GetMapping("/vectorAdd")
    public void vectorAdd() {
        // 1. 加载文档
        Resource resource = new FileSystemResource("src/main/resources/test.txt");
        TextReader textReader = new TextReader(resource);
        List<Document> documents = textReader.get();

        // 2. 分割文档为块
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(64)
                .build();
        List<Document> chunks = splitter.apply(documents);

        // 3. 分批添加到向量存储（每次最多10个）
        int batchSize = 10;
        for (int i = 0; i < chunks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, chunks.size());
            List<Document> batch = chunks.subList(i, end);
            vectorStore.add(batch);
            log.info("已添加批次 {}-{}/{}", i, end, chunks.size());
        }

    }


    @GetMapping("/vectorSearch")
    public String vectorSearch(@RequestParam("query") String query) {
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                        .query(query)
                        .topK(query.length())
                        .build());
        return documents.toString();
    }

    /**
     * 使用 RAG Agent 进行问答
     * Agent 会自动从向量知识库中检索相关信息并生成回答
     *
     * @param question 用户问题
     * @return AI 的回答
     */
    @GetMapping("/chat")
    public String ragChat(@RequestParam("question") String question) {
        try {
            log.info("RAG Agent 收到问题: {}", question);
            Optional<OverAllState> invoke = ragAgent.invoke(question);
            
            if (invoke.isPresent()) {
                String response = invoke.get().toString();
                log.info("RAG Agent 回答完成");
                return response;
            } else {
                return "抱歉，未能生成回答";
            }
        } catch (GraphRunnerException e) {
            log.error("RAG Agent 执行失败", e);
            return "错误: " + e.getMessage();
        }
    }

    /**
     * 使用 RAG Agent 进行带会话上下文的问答
     * 支持多轮对话，每次对话都会保留上下文
     *
     * @param question 用户问题
     * @param userId 用户ID（用于会话隔离）
     * @return AI 的回答
     */
    @GetMapping("/chatWithContext")
    public String ragChatWithContext(@RequestParam("question") String question,
                                     @RequestParam("userId") Long userId) {
        try {
            log.info("RAG Agent 收到问题（用户ID: {}）: {}", userId, question);
            
            RunnableConfig runnableConfig = RunnableConfig.builder()
                    .threadId(userId.toString())
                    .addMetadata("user_id", userId)
                    .build();
            Optional<OverAllState> invoke = ragAgent.invoke(question, runnableConfig);
            
            if (invoke.isPresent()) {
                String response = invoke.get().toString();
                log.info("RAG Agent 回答完成（用户ID: {}）", userId);
                return response;
            } else {
                return "抱歉，未能生成回答";
            }
        } catch (GraphRunnerException e) {
            log.error("RAG Agent 执行失败（用户ID: {}）", userId, e);
            return "错误: " + e.getMessage();
        }
    }
}
