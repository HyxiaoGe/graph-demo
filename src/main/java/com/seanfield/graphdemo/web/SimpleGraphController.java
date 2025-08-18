package com.seanfield.graphdemo.web;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.seanfield.graphdemo.graph.ExpanderNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@RestController
@RequestMapping("/graph")
public class SimpleGraphController {

    private static final Logger log = LoggerFactory.getLogger(SimpleGraphController.class);
    private final StateGraph stateGraph;
    private final ChatClient.Builder chatClientBuilder;

    public SimpleGraphController(ChatClient.Builder chatClientBuilder) throws GraphStateException {
        this.chatClientBuilder = chatClientBuilder;
        log.info("Creating StateGraph with ExpanderNode...");

        // 按照文档要求，创建 OverAllStateFactory
        OverAllStateFactory stateFactory = OverAllState::new;

        // 按照文档要求，使用正确的 StateGraph 构造函数
        this.stateGraph = new StateGraph("Query Expander Workflow", stateFactory)
                .addNode("expander", node_async(new ExpanderNode(chatClientBuilder)))
                .addEdge(StateGraph.START, "expander")
                .addEdge("expander", StateGraph.END);
        log.info("StateGraph created successfully");
    }

    @GetMapping("/expand")
    public Map<String, Object> expand(@RequestParam(value = "query", defaultValue = "你好，很高兴认识你，能简单介绍一下自己吗？") String query,
                                      @RequestParam(value = "expandernumber", defaultValue = "3") Integer expanderNumber,
                                      @RequestParam(value = "threadid", defaultValue = "demo-thread") String threadId) {

        try {
            log.info("Testing basic ChatClient...");
            ExpanderNode testNode = new ExpanderNode(this.chatClientBuilder);

            // 1. 创建 OverAllState，"input" 键已在构造函数中默认注册
            OverAllState testState = new OverAllState();

            // 2. 创建包含业务数据的 payload Map
            Map<String, Object> payload = Map.of("query", query, "expandernumber", expanderNumber);

            // 3. 将 payload Map 作为值，更新到 "input" 键上
            testState.updateState(Map.of("input", payload));

            return testNode.apply(testState);
        } catch (Exception e) {
            log.error("Direct call failed", e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return Map.of("error", errorMsg);
        }
    }
}


