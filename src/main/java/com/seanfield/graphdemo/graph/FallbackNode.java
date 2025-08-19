package com.seanfield.graphdemo.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FallbackNode implements NodeAction {

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		Map<String, Object> input = state.value("input", Map.of());
		String query = String.valueOf(input.getOrDefault("query", ""));
		String reason = "fallback";
		Map<String, Object> validation = state.value("validation", Map.of());
		if (validation.containsKey("reason")) {
			reason = String.valueOf(validation.get("reason"));
		}

		HashMap<String, Object> result = new HashMap<>();
		result.put("error", reason);
		result.put("expandercontent", List.of(query));
		return result;
	}
}



