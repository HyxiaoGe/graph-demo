package com.seanfield.graphdemo.web;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.seanfield.graphdemo.graph.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@RestController
@RequestMapping("/graph")
public class SimpleGraphController {

    private static final Logger log = LoggerFactory.getLogger(SimpleGraphController.class);
    private final StateGraph stateGraph;
    private final StateGraph loopStateGraph;
    private final StateGraph customerServiceGraph;
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

        // 创建循环演示的状态图
        log.info("正在创建循环处理状态图...");
        
        OverAllStateFactory loopStateFactory = () -> {
            OverAllState state = new OverAllState();
            state.registerKeyAndStrategy("items", new ReplaceStrategy());
            state.registerKeyAndStrategy("loop_index", new ReplaceStrategy());
            state.registerKeyAndStrategy("processed_results", new ReplaceStrategy());
            state.registerKeyAndStrategy("loop_completed", new ReplaceStrategy());
            state.registerKeyAndStrategy("current_processing", new ReplaceStrategy());
            state.registerKeyAndStrategy("loop_final_result", new ReplaceStrategy());
            return state;
        };

        this.loopStateGraph = new StateGraph("循环处理工作流", loopStateFactory)
                .addNode("loop_processor", node_async(new LoopProcessorNode(chatClientBuilder)))
                .addNode("result_collector", node_async(new ResultCollectorNode()))
                .addEdge(StateGraph.START, "loop_processor")
                .addConditionalEdges("loop_processor",
                        edge_async(new LoopConditionAction()),
                        Map.of("continue", "loop_processor", "finish", "result_collector"))
                .addEdge("result_collector", StateGraph.END);
        log.info("循环处理状态图创建成功");

        // 创建智能客服工作流状态图
        log.info("正在创建智能客服工作流状态图...");
        
        OverAllStateFactory customerServiceStateFactory = () -> {
            OverAllState state = new OverAllState();
            state.registerKeyAndStrategy("input", new ReplaceStrategy());
            state.registerKeyAndStrategy("intent_analysis", new ReplaceStrategy());
            state.registerKeyAndStrategy("knowledge_search", new ReplaceStrategy());
            state.registerKeyAndStrategy("ai_analysis", new ReplaceStrategy());
            state.registerKeyAndStrategy("generated_answer", new ReplaceStrategy());
            state.registerKeyAndStrategy("quality_assessment", new ReplaceStrategy());
            state.registerKeyAndStrategy("human_service", new ReplaceStrategy());
            state.registerKeyAndStrategy("retry_count", new ReplaceStrategy());
            return state;
        };

        this.customerServiceGraph = new StateGraph("智能客服工作流", customerServiceStateFactory)
                .addNode("intent_recognition", node_async(new IntentRecognitionNode(chatClientBuilder)))
                .addNode("knowledge_search", node_async(new KnowledgeSearchNode()))
                .addNode("ai_analysis", node_async(new AIAnalysisNode(chatClientBuilder)))
                .addNode("human_service", node_async(new HumanServiceNode()))
                .addNode("answer_generation", node_async(new AnswerGenerationNode()))
                .addNode("quality_assessment", node_async(new QualityAssessmentNode(chatClientBuilder)))
                .addEdge(StateGraph.START, "intent_recognition")
                .addConditionalEdges("intent_recognition",
                        edge_async(new IntentRoutingAction()),
                        Map.of("faq", "knowledge_search", "complex", "ai_analysis", "complaint", "human_service"))
                .addEdge("knowledge_search", "answer_generation")
                .addEdge("ai_analysis", "answer_generation")
                .addEdge("answer_generation", "quality_assessment")
                .addConditionalEdges("quality_assessment",
                        edge_async(new QualityCheckAction()),
                        Map.of("pass", StateGraph.END, "retry", "ai_analysis"))
                .addEdge("human_service", StateGraph.END);
        log.info("智能客服工作流状态图创建成功");
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

    @GetMapping("/loop-demo")
    public Map<String, Object> loopDemo(@RequestParam(value = "items", defaultValue = "春天的花朵,夏天的海滩,秋天的落叶,冬天的雪花") String itemsParam) {
        try {
            // 解析输入的项目列表
            List<String> items = Arrays.asList(itemsParam.split(","));
            log.info("开始循环处理演示，项目列表: {}", items);

            // 编译循环状态图
            var compiledLoopGraph = this.loopStateGraph.compile();

            // 创建初始状态
            Map<String, Object> initialData = Map.of(
                    "items", items,
                    "loop_index", 0,
                    "processed_results", List.of()
            );

            // 执行循环状态图
            var result = compiledLoopGraph.invoke(initialData);
            return result.map(state -> {
                Map<String, Object> data = state.data();
                // 返回最终处理结果
                if (data.containsKey("loop_final_result")) {
                    Object finalResult = data.get("loop_final_result");
                    if (finalResult instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> resultMap = (Map<String, Object>) finalResult;
                        return resultMap;
                    }
                }
                // 如果没有最终结果，返回当前状态
                return Map.of(
                        "status", "processing",
                        "current_data", data
                );
            }).orElse(Map.of("error", "循环图执行失败"));

        } catch (Exception e) {
            log.error("循环演示执行失败", e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return Map.of("error", errorMsg);
        }
    }

    @GetMapping("/customer-service")
    public Map<String, Object> customerService(@RequestParam(value = "question", defaultValue = "请问你们的产品有哪些功能？") String question) {
        try {
            log.info("开始智能客服处理，用户问题: {}", question);

            // 编译客服状态图
            var compiledCustomerServiceGraph = this.customerServiceGraph.compile();

            // 创建初始状态
            Map<String, Object> initialData = Map.of(
                    "input", Map.of("question", question),
                    "retry_count", 0
            );

            // 执行客服状态图
            var result = compiledCustomerServiceGraph.invoke(initialData);
            return result.map(state -> {
                Map<String, Object> data = state.data();
                
                // 构建返回结果
                Map<String, Object> response = new HashMap<>();
                
                // 获取最终答案
                if (data.containsKey("generated_answer")) {
                    Object generatedAnswer = data.get("generated_answer");
                    if (generatedAnswer instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> answerMap = (Map<String, Object>) generatedAnswer;
                        response.put("answer", answerMap.get("final_answer"));
                        response.put("answer_source", answerMap.get("answer_source"));
                        response.put("intent_type", answerMap.get("intent_type"));
                    }
                }
                
                // 获取意图分析结果
                if (data.containsKey("intent_analysis")) {
                    response.put("intent_analysis", data.get("intent_analysis"));
                }
                
                // 获取质量评估结果
                if (data.containsKey("quality_assessment")) {
                    response.put("quality_assessment", data.get("quality_assessment"));
                }
                
                // 获取人工客服信息
                if (data.containsKey("human_service")) {
                    response.put("human_service", data.get("human_service"));
                }
                
                response.put("status", "success");
                response.put("processing_time", System.currentTimeMillis());
                
                return response;
                
            }).orElse(Map.of("error", "客服工作流执行失败", "status", "failed"));

        } catch (Exception e) {
            log.error("智能客服处理失败", e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return Map.of("error", errorMsg, "status", "failed");
        }
    }
}


