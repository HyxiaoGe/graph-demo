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
 * 质量评估节点：评估生成答案的质量
 */
public class QualityAssessmentNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(QualityAssessmentNode.class);

    private static final PromptTemplate QUALITY_ASSESSMENT_PROMPT = new PromptTemplate(
            "请你作为客服质量评估专家，评估以下客服回答的质量。\n\n" +
            "用户问题：{question}\n" +
            "客服回答：{answer}\n\n" +
            "请从以下几个维度评估（1-10分）：\n" +
            "1. 准确性：回答是否准确回应了用户问题\n" +
            "2. 完整性：回答是否完整，是否遗漏重要信息\n" +
            "3. 专业性：回答是否专业，用词是否得当\n" +
            "4. 友好性：回答是否友好，语气是否亲切\n" +
            "5. 实用性：回答是否对用户有实际帮助\n\n" +
            "请按以下格式返回评估结果：\n" +
            "准确性：8\n" +
            "完整性：7\n" +
            "专业性：9\n" +
            "友好性：8\n" +
            "实用性：7\n" +
            "总体评分：8\n" +
            "评估理由：回答准确且专业，但可以更详细一些"
    );

    private final ChatClient chatClient;

    public QualityAssessmentNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("QualityAssessmentNode开始执行，当前状态: {}", state.data());

        // 获取用户问题和生成的答案
        Map<String, Object> input = state.value("input", Map.of());
        String question = String.valueOf(input.getOrDefault("question", ""));
        
        Map<String, Object> generatedAnswer = state.value("generated_answer", Map.of());
        String answer = String.valueOf(generatedAnswer.getOrDefault("final_answer", ""));
        
        log.info("正在评估答案质量...");

        // 使用AI进行质量评估
        Flux<String> contentStream = this.chatClient
                .prompt()
                .user(user -> user
                        .text(QUALITY_ASSESSMENT_PROMPT.getTemplate())
                        .param("question", question)
                        .param("answer", answer))
                .stream()
                .content();

        String assessmentResult = contentStream.reduce("", (acc, s) -> acc + s).block();
        
        // 解析评估结果
        QualityScores scores = parseQualityScores(assessmentResult);
        
        log.info("质量评估完成，总体评分: {}", scores.overallScore);

        // 构建返回结果
        HashMap<String, Object> qualityInfo = new HashMap<>();
        qualityInfo.put("accuracy_score", scores.accuracy);
        qualityInfo.put("completeness_score", scores.completeness);
        qualityInfo.put("professionalism_score", scores.professionalism);
        qualityInfo.put("friendliness_score", scores.friendliness);
        qualityInfo.put("usefulness_score", scores.usefulness);
        qualityInfo.put("overall_score", scores.overallScore);
        qualityInfo.put("assessment_reason", scores.reason);
        qualityInfo.put("quality_passed", scores.overallScore >= 7); // 7分以上认为质量合格

        HashMap<String, Object> result = new HashMap<>();
        result.put("quality_assessment", qualityInfo);
        
        return result;
    }

    private QualityScores parseQualityScores(String assessmentResult) {
        QualityScores scores = new QualityScores();
        
        try {
            String[] lines = assessmentResult.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("准确性：")) {
                    scores.accuracy = parseScore(line, "准确性：");
                } else if (line.startsWith("完整性：")) {
                    scores.completeness = parseScore(line, "完整性：");
                } else if (line.startsWith("专业性：")) {
                    scores.professionalism = parseScore(line, "专业性：");
                } else if (line.startsWith("友好性：")) {
                    scores.friendliness = parseScore(line, "友好性：");
                } else if (line.startsWith("实用性：")) {
                    scores.usefulness = parseScore(line, "实用性：");
                } else if (line.startsWith("总体评分：")) {
                    scores.overallScore = parseScore(line, "总体评分：");
                } else if (line.startsWith("评估理由：")) {
                    scores.reason = line.substring("评估理由：".length()).trim();
                }
            }
        } catch (Exception e) {
            log.warn("解析质量评估结果失败，使用默认分数: {}", e.getMessage());
            scores.setDefaultScores();
        }

        return scores;
    }

    private int parseScore(String line, String prefix) {
        try {
            String scoreStr = line.substring(prefix.length()).trim();
            return Integer.parseInt(scoreStr);
        } catch (Exception e) {
            return 7; // 默认分数
        }
    }

    private static class QualityScores {
        int accuracy = 7;
        int completeness = 7;
        int professionalism = 7;
        int friendliness = 7;
        int usefulness = 7;
        int overallScore = 7;
        String reason = "质量评估正常";

        void setDefaultScores() {
            this.accuracy = 7;
            this.completeness = 7;
            this.professionalism = 7;
            this.friendliness = 7;
            this.usefulness = 7;
            this.overallScore = 7;
            this.reason = "默认评分";
        }
    }
}
