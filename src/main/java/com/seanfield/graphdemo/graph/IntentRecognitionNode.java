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
 * 意图识别节点：分析用户问题的意图类型
 */
public class IntentRecognitionNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(IntentRecognitionNode.class);

    private static final PromptTemplate INTENT_RECOGNITION_PROMPT = new PromptTemplate(
            "你是一个专业的客服意图识别专家。请分析用户的问题，判断其意图类型。\n\n" +
            "用户问题：{question}\n\n" +
            "请从以下意图类型中选择最匹配的一个：\n" +
            "1. FAQ - 常见问题（如产品功能、使用方法、价格查询等）\n" +
            "2. COMPLEX - 复杂咨询（需要深度分析的技术问题、业务咨询等）\n" +
            "3. COMPLAINT - 投诉建议（用户不满、问题反馈、改进建议等）\n\n" +
            "请只返回意图类型（FAQ、COMPLEX、COMPLAINT）和置信度（0-100），格式如下：\n" +
            "意图类型：FAQ\n" +
            "置信度：85\n" +
            "理由：用户询问产品的基本功能，属于常见问题"
    );

    private final ChatClient chatClient;

    public IntentRecognitionNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("IntentRecognitionNode开始执行，当前状态: {}", state.data());

        // 获取用户问题
        Map<String, Object> input = state.value("input", Map.of());
        String question = String.valueOf(input.getOrDefault("question", ""));
        
        if (question.trim().isEmpty()) {
            throw new IllegalArgumentException("用户问题不能为空");
        }

        log.info("正在分析用户问题的意图: {}", question);

        // 使用AI进行意图识别
        Flux<String> contentStream = this.chatClient
                .prompt()
                .user(user -> user
                        .text(INTENT_RECOGNITION_PROMPT.getTemplate())
                        .param("question", question))
                .stream()
                .content();

        String analysisResult = contentStream.reduce("", (acc, s) -> acc + s).block();
        
        // 解析AI返回的结果
        IntentAnalysis intent = parseIntentResult(analysisResult);
        
        log.info("意图识别完成: 类型={}, 置信度={}", intent.intentType, intent.confidence);

        // 构建返回结果
        HashMap<String, Object> intentInfo = new HashMap<>();
        intentInfo.put("intent_type", intent.intentType);
        intentInfo.put("confidence", intent.confidence);
        intentInfo.put("reason", intent.reason);
        intentInfo.put("original_question", question);

        HashMap<String, Object> result = new HashMap<>();
        result.put("intent_analysis", intentInfo);
        
        return result;
    }

    private IntentAnalysis parseIntentResult(String analysisResult) {
        IntentAnalysis intent = new IntentAnalysis();
        
        try {
            String[] lines = analysisResult.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("意图类型：")) {
                    String intentType = line.substring("意图类型：".length()).trim();
                    intent.intentType = mapIntentType(intentType);
                } else if (line.startsWith("置信度：")) {
                    String confidenceStr = line.substring("置信度：".length()).trim();
                    try {
                        intent.confidence = Integer.parseInt(confidenceStr);
                    } catch (NumberFormatException e) {
                        intent.confidence = 70; // 默认置信度
                    }
                } else if (line.startsWith("理由：")) {
                    intent.reason = line.substring("理由：".length()).trim();
                }
            }
        } catch (Exception e) {
            log.warn("解析意图识别结果失败，使用默认值: {}", e.getMessage());
            intent.intentType = "COMPLEX";
            intent.confidence = 50;
            intent.reason = "解析失败，默认为复杂咨询";
        }

        return intent;
    }

    private String mapIntentType(String intentType) {
        switch (intentType.toUpperCase()) {
            case "FAQ":
                return "FAQ";
            case "COMPLEX":
                return "COMPLEX";
            case "COMPLAINT":
                return "COMPLAINT";
            default:
                log.warn("未知的意图类型: {}, 默认为COMPLEX", intentType);
                return "COMPLEX";
        }
    }

    private static class IntentAnalysis {
        String intentType = "COMPLEX";
        int confidence = 50;
        String reason = "";
    }
}
