package com.seanfield.graphdemo.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证节点：检查查询内容是否符合要求
 */
public class ValidationNode implements NodeAction {

	private final int minQueryLength;

	public ValidationNode() {
		this.minQueryLength = 1;
	}

	public ValidationNode(int minQueryLength) {
		this.minQueryLength = minQueryLength;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		Map<String, Object> input = state.value("input", Map.of());
		Object queryObj = input.get("query");
		String query = queryObj == null ? "" : String.valueOf(queryObj).trim();

		boolean ok = query.length() >= this.minQueryLength;
		String reason = ok ? "" : "查询内容为空或过短";

		HashMap<String, Object> validation = new HashMap<>();
		validation.put("ok", ok);
		validation.put("reason", reason);

		HashMap<String, Object> result = new HashMap<>();
		result.put("validation", validation);
		return result;
	}
}



