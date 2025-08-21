package com.seanfield.graphdemo.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * AI深度分析节点：处理复杂咨询问题，生成专业回答
 */
public class AIAnalysisNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(AIAnalysisNode.class);

    private static final PromptTemplate AI_ANALYSIS_PROMPT = new PromptTemplate(
            "你是一个专业的客服助手，擅长处理各种复杂的业务咨询问题。\n\n" +
            "用户问题：{question}\n\n" +
            "请根据以下原则提供专业回答：\n" +
            "1. 回答要准确、专业、易懂\n" +
            "2. 如果问题涉及技术细节，请提供详细说明\n" +
            "3. 如果需要进一步沟通，请主动询问\n" +
            "4. 保持友好、耐心的服务态度\n" +
            "5. 回答长度控制在200字以内\n\n" +
            "请提供你的专业回答："
    );

    private final ChatClient chatClient;

    public AIAnalysisNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("AIAnalysisNode开始执行，当前状态: {}", state.data());

        // 获取用户问题
        Map<String, Object> input = state.value("input", Map.of());
        String question = String.valueOf(input.getOrDefault("question", ""));
        
        log.info("AI正在深度分析复杂问题: {}", question);

        // 使用AI进行深度分析
        Flux<String> contentStream = this.chatClient
                .prompt()
                .user(user -> user
                        .text(AI_ANALYSIS_PROMPT.getTemplate())
                        .param("question", question))
                .stream()
                .content();

        String aiResponse = contentStream.reduce("", (acc, s) -> acc + s).block();
        
        log.info("AI深度分析完成，生成回答长度: {}", aiResponse.length());

        // 构建返回结果
        HashMap<String, Object> analysisInfo = new HashMap<>();
        analysisInfo.put("ai_response", aiResponse);
        analysisInfo.put("analysis_type", "complex_inquiry");
        analysisInfo.put("response_length", aiResponse.length());
        analysisInfo.put("source", "ai_analysis");
        analysisInfo.put("original_question", question);

        HashMap<String, Object> result = new HashMap<>();
        result.put("ai_analysis", analysisInfo);
        
        return result;
    }
}
