package com.seanfield.graphdemo.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;

import java.util.Map;

/**
 * 验证条件边：根据验证结果决定下一步流向
 */
public class ValidationEdgeAction implements EdgeAction {

    @Override
    public String apply(OverAllState state) throws Exception {
        Map<String, Object> validation = state.value("validation", Map.of());
        boolean isValid = Boolean.TRUE.equals(validation.getOrDefault("ok", false));
        
        // 返回路径标识：验证通过返回"valid"，失败返回"invalid"
        return isValid ? "valid" : "invalid";
    }
}
