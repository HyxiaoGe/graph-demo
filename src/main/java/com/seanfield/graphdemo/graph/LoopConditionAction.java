package com.seanfield.graphdemo.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 循环条件判断：决定是否继续循环还是结束循环
 */
public class LoopConditionAction implements EdgeAction {

    private static final Logger log = LoggerFactory.getLogger(LoopConditionAction.class);

    @Override
    public String apply(OverAllState state) throws Exception {
        // 获取循环相关状态
        int currentIndex = state.value("loop_index", 0);
        List<String> items = state.value("items", List.of());
        boolean loopCompleted = state.value("loop_completed", false);

        log.info("循环条件检查: 当前索引={}, 总数={}, 是否完成={}", 
                currentIndex, items.size(), loopCompleted);

        // 判断循环条件
        if (loopCompleted || currentIndex >= items.size()) {
            log.info("循环条件: 结束循环，进入结果收集");
            return "finish";  // 循环结束，进入结果收集
        } else {
            log.info("循环条件: 继续循环处理下一个项目");
            return "continue";  // 继续循环
        }
    }
}
