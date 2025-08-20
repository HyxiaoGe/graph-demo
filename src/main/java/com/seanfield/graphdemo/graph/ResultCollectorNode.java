package com.seanfield.graphdemo.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 结果收集节点：收集循环处理的最终结果并格式化输出
 */
public class ResultCollectorNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(ResultCollectorNode.class);

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("ResultCollectorNode开始执行，收集循环处理结果");

        // 获取处理结果
        List<String> processedResults = state.value("processed_results", List.of());
        List<String> originalItems = state.value("items", List.of());
        int totalProcessed = processedResults.size();

        // 生成汇总信息
        String summary = String.format("循环处理完成！共处理 %d 个项目，生成 %d 个分析结果", 
                originalItems.size(), totalProcessed);

        // 创建最终结果
        HashMap<String, Object> finalResult = new HashMap<>();
        finalResult.put("summary", summary);
        finalResult.put("total_items", originalItems.size());
        finalResult.put("processed_count", totalProcessed);
        finalResult.put("original_items", originalItems);
        finalResult.put("analysis_results", processedResults);
        finalResult.put("completion_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        finalResult.put("processing_status", "SUCCESS");

        // 如果有处理失败的项目，计算成功率
        double successRate = originalItems.isEmpty() ? 0.0 : (double) totalProcessed / originalItems.size() * 100;
        finalResult.put("success_rate", String.format("%.1f%%", successRate));

        log.info("循环处理结果收集完成: 处理了 {}/{} 个项目，成功率: {:.1f}%", 
                totalProcessed, originalItems.size(), successRate);

        // 返回收集结果
        HashMap<String, Object> result = new HashMap<>();
        result.put("loop_final_result", finalResult);
        
        return result;
    }
}
