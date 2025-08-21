package com.seanfield.graphdemo.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 答案生成节点：整合不同来源的信息，生成最终回复
 */
public class AnswerGenerationNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(AnswerGenerationNode.class);

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("AnswerGenerationNode开始执行，当前状态: {}", state.data());

        // 获取意图分析结果
        Map<String, Object> intentAnalysis = state.value("intent_analysis", Map.of());
        String intentType = String.valueOf(intentAnalysis.getOrDefault("intent_type", "COMPLEX"));
        
        String finalAnswer;
        String answerSource;
        
        // 根据意图类型选择答案来源
        switch (intentType) {
            case "FAQ":
                finalAnswer = generateFAQAnswer(state);
                answerSource = "knowledge_base";
                break;
            case "COMPLEX":
                finalAnswer = generateComplexAnswer(state);
                answerSource = "ai_analysis";
                break;
            case "COMPLAINT":
                finalAnswer = generateComplaintAnswer(state);
                answerSource = "human_service";
                break;
            default:
                finalAnswer = "抱歉，我无法理解您的问题，请您重新描述或联系人工客服。";
                answerSource = "default";
        }

        log.info("答案生成完成，来源: {}, 长度: {}", answerSource, finalAnswer.length());

        // 构建格式化的最终回复
        String formattedAnswer = formatAnswer(finalAnswer, answerSource);

        HashMap<String, Object> answerInfo = new HashMap<>();
        answerInfo.put("final_answer", formattedAnswer);
        answerInfo.put("answer_source", answerSource);
        answerInfo.put("intent_type", intentType);
        answerInfo.put("generated_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        answerInfo.put("answer_length", formattedAnswer.length());

        HashMap<String, Object> result = new HashMap<>();
        result.put("generated_answer", answerInfo);
        
        return result;
    }

    private String generateFAQAnswer(OverAllState state) {
        Map<String, Object> knowledgeSearch = state.value("knowledge_search", Map.of());
        boolean searchFound = Boolean.TRUE.equals(knowledgeSearch.get("search_found"));
        
        if (searchFound) {
            String faqAnswer = String.valueOf(knowledgeSearch.getOrDefault("faq_answer", ""));
            return faqAnswer;
        } else {
            return "很抱歉，我在知识库中没有找到相关答案。建议您联系我们的客服热线：400-123-4567，我们会为您提供更详细的帮助。";
        }
    }

    private String generateComplexAnswer(OverAllState state) {
        Map<String, Object> aiAnalysis = state.value("ai_analysis", Map.of());
        String aiResponse = String.valueOf(aiAnalysis.getOrDefault("ai_response", ""));
        
        if (!aiResponse.trim().isEmpty()) {
            return aiResponse;
        } else {
            return "这是一个比较复杂的问题，我正在为您分析中。如果您需要更及时的帮助，建议转接人工客服为您详细解答。";
        }
    }

    private String generateComplaintAnswer(OverAllState state) {
        return "非常感谢您的反馈！我们高度重视每一位客户的意见和建议。" +
               "您的问题已经记录，我们的专业客服团队会在24小时内与您联系，" +
               "为您提供满意的解决方案。如需紧急处理，请拨打客服热线：400-123-4567。";
    }

    private String formatAnswer(String answer, String source) {
        StringBuilder formatted = new StringBuilder();
        
        // 添加友好的开头
        formatted.append("😊 ");
        
        // 添加主要内容
        formatted.append(answer);
        
        // 根据来源添加相应的结尾
        switch (source) {
            case "knowledge_base":
                formatted.append("\n\n📚 此答案来自我们的知识库，如有其他疑问，随时为您服务！");
                break;
            case "ai_analysis":
                formatted.append("\n\n🤖 如果还有其他问题，我很乐意继续为您解答！");
                break;
            case "human_service":
                formatted.append("\n\n👥 我们会尽快为您处理，感谢您的耐心！");
                break;
        }
        
        return formatted.toString();
    }
}
