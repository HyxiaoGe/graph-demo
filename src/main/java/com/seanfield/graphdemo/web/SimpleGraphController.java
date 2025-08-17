package com.seanfield.graphdemo.web;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/graph")
public class SimpleGraphController {

	private final CompiledGraph compiledGraph;

	public SimpleGraphController(CompiledGraph compiledGraph) {
		this.compiledGraph = compiledGraph;
	}

	@GetMapping("/expand")
	public Map<String, Object> expand(@RequestParam(value = "query", defaultValue = "你好，很高兴认识你，能简单介绍一下自己吗？") String query,
			@RequestParam(value = "expandernumber", defaultValue = "3") Integer expanderNumber,
			@RequestParam(value = "threadid", defaultValue = "demo-thread") String threadId) {

		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();
		HashMap<String, Object> input = new HashMap<>();
		input.put("query", query);
		input.put("expandernumber", expanderNumber);

		Optional<OverAllState> result = this.compiledGraph.invoke(input, config);
		return result.map(OverAllState::data).orElseGet(HashMap::new);
	}
}


