package com.seanfield.graphdemo.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 意图路由条件：根据意图识别结果决定处理路径
 */
public class IntentRoutingAction implements EdgeAction {

    private static final Logger log = LoggerFactory.getLogger(IntentRoutingAction.class);

    @Override
    public String apply(OverAllState state) throws Exception {
        Map<String, Object> intentAnalysis = state.value("intent_analysis", Map.of());
        String intentType = String.valueOf(intentAnalysis.getOrDefault("intent_type", "COMPLEX"));
        int confidence = (Integer) intentAnalysis.getOrDefault("confidence", 50);

        log.info("意图路由判断: 类型={}, 置信度={}", intentType, confidence);

        // 如果置信度太低，转为复杂咨询处理
        if (confidence < 60) {
            log.info("置信度过低，转为复杂咨询处理");
            return "complex";
        }

        // 根据意图类型进行路由
        switch (intentType.toUpperCase()) {
            case "FAQ":
                log.info("路由到知识库搜索");
                return "faq";
            case "COMPLEX":
                log.info("路由到AI深度分析");
                return "complex";
            case "COMPLAINT":
                log.info("路由到人工客服");
                return "complaint";
            default:
                log.info("未知意图类型，默认路由到复杂咨询");
                return "complex";
        }
    }
}
