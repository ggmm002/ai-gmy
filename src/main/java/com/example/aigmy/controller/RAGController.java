package com.example.aigmy.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
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
    private MilvusVectorStore milvusVectorStore;


    @GetMapping("/vectorSearch")
    public String vectorSearch(@RequestParam("query") String query) {
        List<Document> documents = milvusVectorStore.similaritySearch(SearchRequest.builder()
                        .query(query)
                        .topK(query.length())
                        .build());
        return documents.toString();
    }
}
