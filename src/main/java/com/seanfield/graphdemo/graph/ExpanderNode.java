package com.seanfield.graphdemo.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 扩展节点：使用AI生成查询的多个变体版本
 */
public class ExpanderNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(ExpanderNode.class);

    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate(
            "You are an expert at information retrieval and search optimization.\n" +
                    "Your task is to generate {number} different versions of the given query.\n\n" +
                    "Each variant must cover different perspectives or aspects of the topic,\n" +
                    "while maintaining the core intent of the original query. The goal is to\n" +
                    "expand the search space and improve the chances of finding relevant information.\n\n" +
                    "Do not explain your choices or add any other text.\n" +
                    "Provide the query variants separated by newlines.\n\n" +
                    "Original query: {query}\n\n" +
                    "Query variants:\n"
    );

    private final ChatClient chatClient;
    private final Integer defaultNumber = 3;

    public ExpanderNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("ExpanderNode开始执行，当前状态: {}", state.data());
        
        // 1. 从状态中获取输入数据
        Map<String, Object> payload = state.value("input", Map.of());

        // 2. 提取查询参数
        String query = (String) payload.getOrDefault("query", "");
        Integer expanderNumber = (Integer) payload.getOrDefault("expandernumber", this.defaultNumber);

        log.info("处理查询: {}, 扩展数量: {}", query, expanderNumber);

        Flux<String> contentStream = this.chatClient
                .prompt()
                .user(user -> user
                        .text(DEFAULT_PROMPT_TEMPLATE.getTemplate())
                        .param("number", expanderNumber)
                        .param("query", query))
                .stream()
                .content();

        String all = contentStream.reduce("", (acc, s) -> acc + s).block();
        List<String> variants = List.of(all.split("\n"));

        HashMap<String, Object> result = new HashMap<>();
        result.put("expandercontent", variants);
        return result;
    }
}


