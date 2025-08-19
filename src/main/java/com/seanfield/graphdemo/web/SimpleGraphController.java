package com.seanfield.graphdemo.web;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.seanfield.graphdemo.graph.ExpanderNode;
import com.seanfield.graphdemo.graph.FallbackNode;
import com.seanfield.graphdemo.graph.ValidationEdgeAction;
import com.seanfield.graphdemo.graph.ValidationNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@RestController
@RequestMapping("/graph")
public class SimpleGraphController {

    private static final Logger log = LoggerFactory.getLogger(SimpleGraphController.class);
    private final StateGraph stateGraph;
    private final ChatClient.Builder chatClientBuilder;

    public SimpleGraphController(ChatClient.Builder chatClientBuilder) throws GraphStateException {
        this.chatClientBuilder = chatClientBuilder;
        log.info("正在创建包含扩展节点的状态图...");

        // 创建预配置所有必要键的状态工厂
        OverAllStateFactory stateFactory = () -> {
            OverAllState state = new OverAllState();
            state.registerKeyAndStrategy("validation", new ReplaceStrategy());
            state.registerKeyAndStrategy("expandercontent", new ReplaceStrategy());
            state.registerKeyAndStrategy("error", new ReplaceStrategy());
            return state;
        };

        // 创建包含条件逻辑的状态图
        this.stateGraph = new StateGraph("条件扩展工作流", stateFactory)
                .addNode("validation", node_async(new ValidationNode(1)))
                .addNode("expander", node_async(new ExpanderNode(chatClientBuilder)))
                .addNode("fallback", node_async(new FallbackNode()))
                .addEdge(StateGraph.START, "validation")
                .addConditionalEdges("validation",
                        edge_async(new ValidationEdgeAction()),
                        Map.of("valid", "expander", "invalid", "fallback"))
                .addEdge("expander", StateGraph.END)
                .addEdge("fallback", StateGraph.END);
        log.info("状态图创建成功");
    }

    @GetMapping("/expand")
    public Map<String, Object> expand(@RequestParam(value = "query", defaultValue = "你好，很高兴认识你，能简单介绍一下自己吗？") String query,
                                      @RequestParam(value = "expandernumber", defaultValue = "3") Integer expanderNumber,
                                      @RequestParam(value = "threadid", defaultValue = "demo-thread") String threadId) {

        try {
            ExpanderNode expanderNode = new ExpanderNode(this.chatClientBuilder);
            OverAllState state = new OverAllState();
            Map<String, Object> payload = Map.of("query", query, "expandernumber", expanderNumber);
            state.updateState(Map.of("input", payload));
            return expanderNode.apply(state);
        } catch (Exception e) {
            log.error("直接调用失败", e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return Map.of("error", errorMsg);
        }
    }

    @GetMapping("/expand-conditional")
    public Map<String, Object> expandConditional(@RequestParam(value = "query", defaultValue = "") String query,
                                                 @RequestParam(value = "expandernumber", defaultValue = "3") Integer expanderNumber) {
        try {
            // 编译现有的状态图
            var compiledGraph = this.stateGraph.compile();

            // 创建初始状态并设置输入数据
            Map<String, Object> initialData = Map.of("input", Map.of("query", query, "expandernumber", expanderNumber));

            // 执行图并返回结果
            var result = compiledGraph.invoke(initialData);
            return result.map(state -> {
                Map<String, Object> data = state.data();
                // 如果有 expandercontent，直接返回它
                if (data.containsKey("expandercontent")) {
                    return Map.of("expandercontent", data.get("expandercontent"));
                }
                // 如果有 error，返回错误信息
                if (data.containsKey("error")) {
                    return Map.of("error", data.get("error"), "expandercontent", data.get("expandercontent"));
                }
                // 其他情况返回完整数据
                return data;
            }).orElse(Map.of("error", "图执行失败"));
        } catch (Exception e) {
            log.error("条件流程执行失败", e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return Map.of("error", errorMsg);
        }
    }
}


