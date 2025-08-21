package com.seanfield.graphdemo.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 质量检查条件：根据质量评估结果决定是否需要重新生成答案
 */
public class QualityCheckAction implements EdgeAction {

    private static final Logger log = LoggerFactory.getLogger(QualityCheckAction.class);

    @Override
    public String apply(OverAllState state) throws Exception {
        Map<String, Object> qualityAssessment = state.value("quality_assessment", Map.of());
        boolean qualityPassed = Boolean.TRUE.equals(qualityAssessment.get("quality_passed"));
        int overallScore = (Integer) qualityAssessment.getOrDefault("overall_score", 7);
        
        // 获取重试次数，避免无限循环
        int retryCount = state.value("retry_count", 0);
        
        log.info("质量检查: 质量合格={}, 总分={}, 重试次数={}", qualityPassed, overallScore, retryCount);

        // 如果重试次数超过2次，强制通过
        if (retryCount >= 2) {
            log.info("已达到最大重试次数，强制通过质量检查");
            return "pass";
        }

        // 根据质量评估结果决定路径
        if (qualityPassed && overallScore >= 7) {
            log.info("质量检查通过，继续发送回复");
            return "pass";
        } else {
            log.info("质量检查未通过，需要重新生成答案");
            // 增加重试计数
            state.updateState(Map.of("retry_count", retryCount + 1));
            return "retry";
        }
    }
}
