package com.seanfield.graphdemo.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.nodeasync;

@Configuration
public class GraphConfiguration {

	private static final Logger log = LoggerFactory.getLogger(GraphConfiguration.class);

	@Bean(name = "simpleGraph")
	public StateGraph simpleGraph(@Qualifier("qwenChatClientBuilder") ChatClient.Builder qwenChatClientBuilder) throws GraphStateException {
		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("query", new ReplaceStrategy());
			strategies.put("expandernumber", new ReplaceStrategy());
			strategies.put("expandercontent", new ReplaceStrategy());
			return strategies;
		};

		StateGraph graph = new StateGraph(keyStrategyFactory)
			.addNode("expander", nodeasync(new ExpanderNode(qwenChatClientBuilder)))
			.addEdge(StateGraph.START, "expander")
			.addEdge("expander", StateGraph.END);

		GraphRepresentation uml = graph.getGraph(GraphRepresentation.Type.PLANTUML, "expander flow");
		log.info("\n=== Graph UML ===\n{}\n===================\n", uml.content());

		return graph;
	}

	@Bean
	public CompiledGraph compiledGraph(StateGraph simpleGraph) throws GraphStateException {
		return simpleGraph.compile();
	}
}


