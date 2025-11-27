package com.example.aigmy.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
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
}
