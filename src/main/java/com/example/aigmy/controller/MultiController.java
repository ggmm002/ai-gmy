package com.example.aigmy.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/multi")
@Slf4j
public class MultiController {


    @Autowired
    private ReactAgent multiAgent;


    @GetMapping("/chat")
    public String chat(@RequestParam("question") String question,@RequestParam("userId") Long userId ) throws GraphRunnerException {
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId(userId.toString()) // 暂时先用userId
                .addMetadata("user_id", userId)
                .build();

        Optional<OverAllState> invoke = multiAgent.invoke(question, runnableConfig);
        return invoke.get().toString();
    }
}
