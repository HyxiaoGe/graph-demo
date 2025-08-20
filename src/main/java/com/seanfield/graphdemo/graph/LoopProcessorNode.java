package com.seanfield.graphdemo.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 循环处理节点：在循环中对每个项目执行AI处理
 */
public class LoopProcessorNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(LoopProcessorNode.class);

    private static final PromptTemplate ANALYSIS_PROMPT = new PromptTemplate(
            "请分析以下文本内容，提供一个简短的见解和评价：\n\n" +
            "文本：{text}\n\n" +
            "请返回你的分析结果（控制在50字以内）："
    );

    private final ChatClient chatClient;

    public LoopProcessorNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("LoopProcessorNode开始执行，当前状态: {}", state.data());

        // 获取当前循环索引和待处理项目列表
        int currentIndex = state.value("loop_index", 0);
        List<String> items = state.value("items", List.of());
        List<String> processedResults = state.value("processed_results", new ArrayList<>());

        log.info("处理第 {} 个项目，总数: {}", currentIndex + 1, items.size());

        if (currentIndex < items.size()) {
            String currentItem = items.get(currentIndex);
            
            // 使用AI处理当前项目
            Flux<String> contentStream = this.chatClient
                    .prompt()
                    .user(user -> user
                            .text(ANALYSIS_PROMPT.getTemplate())
                            .param("text", currentItem))
                    .stream()
                    .content();

            String analysisResult = contentStream.reduce("", (acc, s) -> acc + s).block();
            
            // 添加到已处理结果中
            List<String> newProcessedResults = new ArrayList<>(processedResults);
            newProcessedResults.add(String.format("项目 %d: %s -> 分析: %s", 
                    currentIndex + 1, currentItem, analysisResult));

            // 更新状态
            HashMap<String, Object> result = new HashMap<>();
            result.put("loop_index", currentIndex + 1);
            result.put("processed_results", newProcessedResults);
            result.put("current_processing", String.format("已处理 %d/%d 个项目", 
                    currentIndex + 1, items.size()));

            log.info("完成第 {} 个项目的处理", currentIndex + 1);
            return result;
        }

        // 如果所有项目都处理完成
        HashMap<String, Object> result = new HashMap<>();
        result.put("loop_completed", true);
        result.put("final_results", processedResults);
        result.put("current_processing", String.format("所有 %d 个项目处理完成", items.size()));
        
        log.info("所有项目处理完成，共处理 {} 个项目", items.size());
        return result;
    }
}
