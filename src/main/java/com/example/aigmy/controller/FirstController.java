package com.example.aigmy.controller;

import com.alibaba.cloud.ai.graph.OverAllState;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FirstController {

    @Autowired
    private ReactAgent firstAgent;

    @GetMapping("/chat")
    public String getChatResponse(@RequestParam("question") String question) {
        Optional<OverAllState> invoke;
        try {
            invoke = firstAgent.invoke(question);
        } catch (GraphRunnerException e) {
            return "Error: " + e.getMessage();
        }
        if (invoke.isPresent()) {
            return invoke.get().toString();
        } else {
            return "No response";
        }
    }

    @GetMapping("/chat2")
    public String getChatResponse2(@RequestParam("question") String question,@RequestParam("userId") Long userId ) {
        Optional<OverAllState> invoke;
        RunnableConfig runnableConfig = RunnableConfig.builder()
        .threadId(userId.toString()) // 暂时先用userId
        .addMetadata("user_id", userId)
                .build();
        try {
            invoke = firstAgent.invoke(question,runnableConfig);
        } catch (GraphRunnerException e) {
            return "Error: " + e.getMessage();
        }
        if (invoke.isPresent()) {
            return invoke.get().toString();
        } else {
            return "No response";
        }
    }
}
