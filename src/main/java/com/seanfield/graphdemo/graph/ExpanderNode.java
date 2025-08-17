package com.seanfield.graphdemo.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpanderNode implements NodeAction {

	private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate(
			"You are an expert at information retrieval and search optimization.\n" +
			"Your task is to generate {number} different versions of the given query.\n\n" +
			"Each variant must cover different perspectives or aspects of the topic,\n" +
			"while maintaining the core intent of the original query. The goal is to\n" +
			"expand the search space and improve the chances of finding relevant information.\n\n" +
			"Do not explain your choices or add any other text.\n" +
			"Provide the query variants separated by newlines.\n\n" +
			"Original query: {query}\n\n" +
			"Query variants:\n"
	);

	private final ChatClient chatClient;
	private final Integer defaultNumber = 3;

	public ExpanderNode(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		String query = state.value("query", "");
		Integer expanderNumber = state.value("expandernumber", this.defaultNumber);

		Flux<String> contentStream = this.chatClient
			.prompt()
			.user(user -> user
				.text(DEFAULT_PROMPT_TEMPLATE.getTemplate())
				.param("number", expanderNumber)
				.param("query", query))
			.stream()
			.content();

		String all = contentStream.reduce("", (acc, s) -> acc + s).block();
		List<String> variants = List.of(all.split("\n"));

		HashMap<String, Object> result = new HashMap<>();
		result.put("expandercontent", variants);
		return result;
	}
}


