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
 * 人工客服节点：处理需要人工介入的问题
 */
public class HumanServiceNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(HumanServiceNode.class);

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("HumanServiceNode开始执行，当前状态: {}", state.data());

        // 获取意图分析结果
        Map<String, Object> intentAnalysis = state.value("intent_analysis", Map.of());
        String intentType = String.valueOf(intentAnalysis.getOrDefault("intent_type", ""));

        log.info("转接人工客服处理: 问题类型={}", intentType);

        // 生成人工客服工单号
        String ticketId = generateTicketId();
        
        // 根据问题类型生成相应的回复
        String humanServiceReply = generateHumanServiceReply(intentType, ticketId);

        HashMap<String, Object> humanServiceInfo = new HashMap<>();
        humanServiceInfo.put("ticket_id", ticketId);
        humanServiceInfo.put("service_type", "human_service");
        humanServiceInfo.put("expected_response_time", "30分钟内");
        humanServiceInfo.put("contact_method", "电话回访或在线客服");
        humanServiceInfo.put("priority", getPriority(intentType));
        humanServiceInfo.put("assigned_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 构建最终回复
        HashMap<String, Object> answerInfo = new HashMap<>();
        answerInfo.put("final_answer", humanServiceReply);
        answerInfo.put("answer_source", "human_service");
        answerInfo.put("intent_type", intentType);
        answerInfo.put("generated_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        answerInfo.put("answer_length", humanServiceReply.length());

        HashMap<String, Object> result = new HashMap<>();
        result.put("human_service", humanServiceInfo);
        result.put("generated_answer", answerInfo);
        
        return result;
    }

    private String generateTicketId() {
        return "CS" + System.currentTimeMillis() % 1000000;
    }

    private String generateHumanServiceReply(String intentType, String ticketId) {
        StringBuilder reply = new StringBuilder();
        reply.append("😊 您好！感谢您联系我们的客服。\n\n");

        switch (intentType.toUpperCase()) {
            case "COMPLAINT":
                reply.append("我们非常重视您的投诉和建议。您的问题已经记录在我们的系统中，工单号为：")
                     .append(ticketId)
                     .append("。\n\n")
                     .append("我们的专业客服团队会在30分钟内与您联系，为您提供满意的解决方案。");
                break;
            case "COMPLEX":
                reply.append("您的问题比较复杂，需要我们的技术专家为您详细解答。工单号：")
                     .append(ticketId)
                     .append("。\n\n")
                     .append("我们会安排最合适的专家在30分钟内为您提供专业咨询。");
                break;
            default:
                reply.append("您的咨询已转接给人工客服，工单号：")
                     .append(ticketId)
                     .append("。\n\n")
                     .append("我们会尽快为您处理，预计30分钟内回复。");
        }

        reply.append("\n\n📞 如需紧急处理，请直接拨打客服热线：400-123-4567")
             .append("\n💬 您也可以继续在此等待，我们的客服会主动联系您")
             .append("\n\n感谢您的耐心等待！");

        return reply.toString();
    }

    private String getPriority(String intentType) {
        switch (intentType.toUpperCase()) {
            case "COMPLAINT":
                return "高优先级";
            case "COMPLEX":
                return "中优先级";
            default:
                return "普通优先级";
        }
    }
}
