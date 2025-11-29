package com.example.aigmy.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 向量搜索工具，用于从向量数据库中检索相关文档
 * 
 * @author guomaoyang
 */
@Slf4j
@Component
public class VectorSearchTool implements BiFunction<String, ToolContext, String> {

    @Autowired
    private MilvusVectorStore vectorStore;

    // 默认返回的文档数量
    private static final int DEFAULT_TOP_K = 5;

    @Override
    public String apply(@ToolParam(description = "用户查询的问题或关键词") String query, ToolContext toolContext) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return "查询内容不能为空";
            }

            log.info("执行向量搜索，查询内容: {}", query);

            // 执行向量相似度搜索
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(DEFAULT_TOP_K)
                    .build();

            List<Document> documents = vectorStore.similaritySearch(searchRequest);

            if (documents == null || documents.isEmpty()) {
                log.info("未找到相关文档");
                return "未在知识库中找到相关信息";
            }

            // 将文档内容合并为字符串返回
            String result = documents.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n---\n\n"));

            log.info("向量搜索完成，找到 {} 个相关文档", documents.size());
            return result;

        } catch (Exception e) {
            log.error("向量搜索失败", e);
            return "向量搜索执行失败: " + e.getMessage();
        }
    }
}


