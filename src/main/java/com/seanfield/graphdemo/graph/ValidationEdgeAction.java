package com.seanfield.graphdemo.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;

import java.util.Map;

/**
 * 验证条件边：根据validation结果决定下一步流向
 */
public class ValidationEdgeAction implements EdgeAction {

    @Override
    public String apply(OverAllState state) throws Exception {
        Map<String, Object> validation = state.value("validation", Map.of());
        boolean isValid = Boolean.TRUE.equals(validation.getOrDefault("ok", false));
        
        return isValid ? "valid" : "invalid";
    }
}
